import * as sdk from "matrix-js-sdk";
import { decodeRecoveryKey } from "matrix-js-sdk/lib/crypto-api/recovery-key.js";

const nativeInstantiateStreaming = WebAssembly.instantiateStreaming?.bind(WebAssembly);
WebAssembly.instantiateStreaming = async (source, imports) => {
  if (nativeInstantiateStreaming) {
    try {
      return await nativeInstantiateStreaming(source, imports);
    } catch (_) {
    }
  }
  const response = await source;
  const bytes = await response.arrayBuffer();
  return WebAssembly.instantiate(bytes, imports);
};

let client = null;
let store = null;
let sessionKey = "";
let recoveryKey = "";
let starting = null;

function post(type, payload = {}, message = "") {
  const target = globalThis.AndroidMatrixRuntime;
  if (!target || typeof target.postMessage !== "function") {
    return;
  }
  target.postMessage(JSON.stringify({ type, payload, message }));
}

function status(message) {
  post("status", {}, message);
}

function fail(error) {
  const message = error && error.message ? error.message : String(error);
  post("error", {}, message);
}

function normalizeHomeserver(value) {
  let result = String(value || "").trim() || "https://matrix.org";
  if (!/^https?:\/\//i.test(result)) {
    result = `https://${result}`;
  }
  return result.replace(/\/+$/, "");
}

function cleanId(value) {
  return String(value || "matrix")
    .replace(/[^a-zA-Z0-9_.-]/g, "_")
    .slice(0, 80);
}

function roomInitials(title) {
  const clean = String(title || "").trim();
  return clean ? clean.slice(0, 1).toUpperCase() : "M";
}

function messageTime(event) {
  const date = event.getDate?.();
  if (!date) {
    return "";
  }
  return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
}

function bodyFromContent(content) {
  if (!content) {
    return "";
  }
  if (typeof content.body === "string" && content.body.trim()) {
    return content.body;
  }
  if (typeof content.formatted_body === "string" && content.formatted_body.trim()) {
    return content.formatted_body.replace(/<br\s*\/?>/gi, "\n").replace(/<[^>]+>/g, "");
  }
  return "";
}

function nativeMessageFromEvent(event) {
  if (!event || event.getType?.() !== "m.room.message") {
    return null;
  }
  const content = event.getContent?.() || {};
  let body = bodyFromContent(content);
  if (!body && event.isEncrypted?.()) {
    body = event.isDecryptionFailure?.() ? "Unable to decrypt message" : "Encrypted message, waiting for keys";
  }
  if (!body) {
    return null;
  }
  return {
    sender: event.getSender?.() || "Matrix",
    time: messageTime(event),
    bodyMarkdown: body,
    outbound: event.getSender?.() === client?.getUserId?.(),
  };
}

function snapshotPayload() {
  if (!client) {
    return { nextBatch: "", rooms: [] };
  }
  const rooms = client.getRooms()
    .filter((room) => room && room.roomId)
    .map((room) => {
      const events = room.getLiveTimeline().getEvents();
      const messages = events
        .map(nativeMessageFromEvent)
        .filter(Boolean)
        .slice(-80);
      const subtitle = messages.length ? messages[messages.length - 1].bodyMarkdown : "";
      const title = room.name || room.roomId;
      return {
        id: room.roomId,
        title,
        subtitle,
        initials: roomInitials(title),
        unreadCount: room.getUnreadNotificationCount?.() || 0,
        encrypted: room.hasEncryptionStateEvent?.() || false,
        messages,
      };
    });
  return { nextBatch: client.store?.getSyncToken?.() || "", rooms };
}

function emitSnapshot() {
  post("snapshot", snapshotPayload());
}

function installListeners() {
  client.on(sdk.ClientEvent.Sync, (state, _previous, data) => {
    status(`Sync ${state}`);
    if (state === sdk.SyncState.Prepared || state === sdk.SyncState.Syncing) {
      emitSnapshot();
    } else if (state === sdk.SyncState.Error && data?.error) {
      fail(data.error);
    }
  });
  client.on(sdk.RoomEvent.Timeline, () => emitSnapshot());
  client.on(sdk.MatrixEventEvent.Decrypted, () => emitSnapshot());
}

async function stopClient() {
  if (client) {
    client.stopClient();
    client.removeAllListeners();
  }
  if (store) {
    await store.save(true).catch(() => {});
    await store.destroy().catch(() => {});
  }
  client = null;
  store = null;
}

async function createCryptoCallbacks() {
  return {
    getSecretStorageKey: async ({ keys }) => {
      if (!recoveryKey.trim()) {
        return null;
      }
      const keyIds = Object.keys(keys || {});
      if (!keyIds.length) {
        return null;
      }
      let keyId = null;
      try {
        const defaultKeyId = await client?.secretStorage?.getDefaultKeyId?.();
        if (defaultKeyId && keys[defaultKeyId]) {
          keyId = defaultKeyId;
        }
      } catch (_) {
      }
      keyId = keyId || keyIds[0];
      return [keyId, decodeRecoveryKey(recoveryKey.trim())];
    },
  };
}

async function unlockBackupIfPossible() {
  if (!client || !recoveryKey.trim()) {
    return;
  }
  const crypto = client.getCrypto?.();
  if (!crypto) {
    return;
  }
  await crypto.loadSessionBackupPrivateKeyFromSecretStorage();
  await crypto.checkKeyBackupAndEnable();
  status("E2EE key backup enabled");
}

async function startSession(payload) {
  const homeserver = normalizeHomeserver(payload.homeserver);
  const accessToken = String(payload.accessToken || "").trim();
  const userId = String(payload.userId || "").trim();
  const deviceId = String(payload.deviceId || "").trim();
  recoveryKey = String(payload.recoveryKey || "");
  if (!accessToken || !userId || !deviceId) {
    throw new Error("Matrix JS E2EE runtime needs access token, user ID, and device ID.");
  }
  const nextSessionKey = `${homeserver}\n${userId}\n${deviceId}\n${accessToken}`;
  if (client && sessionKey === nextSessionKey) {
    emitSnapshot();
    return;
  }
  if (starting) {
    await starting;
    if (client && sessionKey === nextSessionKey) {
      emitSnapshot();
      return;
    }
  }
  starting = (async () => {
    await stopClient();
    sessionKey = nextSessionKey;
    store = new sdk.IndexedDBStore({
      indexedDB: globalThis.indexedDB,
      localStorage: globalThis.localStorage,
      dbName: `matrix-rich-store-${cleanId(userId)}-${cleanId(deviceId)}`,
    });
    client = sdk.createClient({
      baseUrl: homeserver,
      accessToken,
      userId,
      deviceId,
      store,
      timelineSupport: true,
      cryptoCallbacks: await createCryptoCallbacks(),
    });
    await store.startup();
    await client.initRustCrypto({
      useIndexedDB: true,
      cryptoDatabasePrefix: `matrix-rich-crypto-${cleanId(userId)}-${cleanId(deviceId)}`,
    });
    await unlockBackupIfPossible().catch((error) => status(`E2EE backup unavailable: ${error.message}`));
    installListeners();
    await client.startClient({
      initialSyncLimit: 30,
      lazyLoadMembers: true,
      disablePresence: true,
      pollTimeout: 30000,
    });
    status("Matrix JS E2EE runtime started");
  })();
  await starting;
}

async function loginPassword(payload) {
  const homeserver = normalizeHomeserver(payload.homeserver);
  recoveryKey = String(payload.recoveryKey || "");
  const loginClient = sdk.createClient({ baseUrl: homeserver });
  const result = await loginClient.loginWithPassword(payload.account, payload.password);
  const loginPayload = {
    accessToken: result.access_token,
    userId: result.user_id,
    deviceId: result.device_id,
  };
  post("login", loginPayload);
  await startSession({
    homeserver,
    accessToken: loginPayload.accessToken,
    userId: loginPayload.userId,
    deviceId: loginPayload.deviceId,
    recoveryKey,
  });
}

async function sendText(payload) {
  if (!client) {
    throw new Error("Matrix runtime is not signed in.");
  }
  const roomId = String(payload.roomId || "");
  const body = String(payload.body || "");
  await client.sendTextMessage(roomId, body);
  post("send", { roomId, body });
  emitSnapshot();
}

async function dispatch(rawCommand) {
  try {
    const command = JSON.parse(rawCommand);
    const payload = command.payload || {};
    if (command.op === "loginPassword") {
      await loginPassword(payload);
    } else if (command.op === "startSession") {
      await startSession(payload);
    } else if (command.op === "snapshot") {
      emitSnapshot();
    } else if (command.op === "sendText") {
      await sendText(payload);
    } else {
      throw new Error(`Unknown runtime operation: ${command.op}`);
    }
  } catch (error) {
    fail(error);
  }
}

globalThis.MatrixRichRuntime = { dispatch };
post("ready");
