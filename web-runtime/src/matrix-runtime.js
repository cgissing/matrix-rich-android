import * as sdk from "matrix-js-sdk";
import { CryptoEvent, VerificationPhase, VerificationRequestEvent, VerifierEvent } from "matrix-js-sdk/lib/crypto-api/index.js";
import { VerificationMethod } from "matrix-js-sdk/lib/types.js";
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
let currentVerificationRequest = null;
let currentVerificationSource = "";
let currentVerifier = null;
let currentSas = null;
let currentQrCodeBase64 = "";
let currentQrReciprocate = null;
const runningVerifiers = new WeakSet();

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

export function phaseName(phase) {
  return VerificationPhase[phase] || "Unknown";
}

export function normalizeHomeserver(value) {
  let result = String(value || "").trim() || "https://matrix.org";
  if (!/^https?:\/\//i.test(result)) {
    result = `https://${result}`;
  }
  return result.replace(/\/+$/, "");
}

async function fetchJson(url) {
  if (typeof fetch !== "function") {
    return null;
  }
  try {
    const response = await fetch(url, {
      cache: "no-store",
      credentials: "omit",
    });
    if (!response.ok) {
      return null;
    }
    return await response.json();
  } catch (_) {
    return null;
  }
}

function elementWebHomeserverBase(config) {
  return config?.default_server_config?.["m.homeserver"]?.base_url
    || config?.default_hs_url
    || "";
}

function wellKnownHomeserverBase(config) {
  return config?.["m.homeserver"]?.base_url || "";
}

function isMatrixVersionsResponse(config) {
  return Array.isArray(config?.versions);
}

export async function resolveHomeserver(value) {
  const input = normalizeHomeserver(value);
  const elementConfig = await fetchJson(`${input}/config.json`);
  const elementBase = elementWebHomeserverBase(elementConfig);
  if (elementBase) {
    const baseUrl = normalizeHomeserver(elementBase);
    return {
      input,
      baseUrl,
      source: baseUrl === input ? "direct" : "element-web-config",
    };
  }

  if (isMatrixVersionsResponse(await fetchJson(`${input}/_matrix/client/versions`))) {
    return {
      input,
      baseUrl: input,
      source: "direct",
    };
  }

  try {
    const origin = new URL(input).origin;
    const wellKnown = await fetchJson(`${origin}/.well-known/matrix/client`);
    const wellKnownBase = wellKnownHomeserverBase(wellKnown);
    if (wellKnownBase) {
      const baseUrl = normalizeHomeserver(wellKnownBase);
      return {
        input,
        baseUrl,
        source: baseUrl === input ? "direct" : "well-known",
      };
    }
  } catch (_) {
  }

  return {
    input,
    baseUrl: input,
    source: "direct",
  };
}

export function sasPayloadFromCallbacks(callbacks) {
  const sas = callbacks?.sas || {};
  const decimal = Array.isArray(sas.decimal) ? sas.decimal.join(" ") : "";
  const emoji = Array.isArray(sas.emoji)
    ? sas.emoji.map((entry) => `${entry?.[0] || ""} ${entry?.[1] || ""}`.trim()).filter(Boolean).join("\n")
    : "";
  return {
    sasDecimal: decimal,
    sasEmoji: emoji,
    canConfirmSas: true,
  };
}

export function bytesToBase64(bytes) {
  if (!bytes || !bytes.length) {
    return "";
  }
  if (typeof Buffer !== "undefined") {
    return Buffer.from(bytes).toString("base64");
  }
  let binary = "";
  const chunkSize = 0x8000;
  for (let offset = 0; offset < bytes.length; offset += chunkSize) {
    const chunk = bytes.subarray(offset, offset + chunkSize);
    binary += String.fromCharCode(...chunk);
  }
  return btoa(binary);
}

export function base64ToBytes(value) {
  const base64 = String(value || "");
  if (!base64) {
    return new Uint8ClampedArray();
  }
  if (typeof Buffer !== "undefined") {
    return new Uint8ClampedArray(Buffer.from(base64, "base64"));
  }
  const binary = atob(base64);
  const bytes = new Uint8ClampedArray(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes;
}

function requestMethods(request) {
  try {
    const methods = request?.methods;
    return Array.isArray(methods) ? methods : [];
  } catch (_) {
    return [];
  }
}

function requestSupportsMethod(request, method) {
  try {
    return !!request?.otherPartySupportsMethod?.(method);
  } catch (_) {
    return false;
  }
}

export function canShowQrFromRequest(request) {
  const phaseCode = Number(request?.phase || 0);
  return phaseCode === VerificationPhase.Ready
    && typeof request?.generateQRCode === "function"
    && requestSupportsMethod(request, VerificationMethod.ScanQrCode);
}

export function canScanQrFromRequest(request) {
  const phaseCode = Number(request?.phase || 0);
  return phaseCode === VerificationPhase.Ready
    && typeof request?.scanQRCode === "function"
    && requestSupportsMethod(request, VerificationMethod.ShowQrCode);
}

export function verificationPayloadFromRequest(request, source, extra = {}) {
  const phaseCode = Number(request?.phase || 0);
  const canAccept = phaseCode <= VerificationPhase.Requested
    && !request?.accepting
    && !request?.declining;
  return {
    transactionId: request?.transactionId || "",
    roomId: request?.roomId || "",
    initiatedByMe: !!request?.initiatedByMe,
    otherUserId: request?.otherUserId || "",
    otherDeviceId: request?.otherDeviceId || "",
    isSelfVerification: !!request?.isSelfVerification,
    phaseCode,
    phase: phaseName(phaseCode),
    pending: !!request?.pending,
    accepting: !!request?.accepting,
    declining: !!request?.declining,
    timeout: request?.timeout || 0,
    methods: requestMethods(request),
    chosenMethod: request?.chosenMethod || "",
    source: source || "",
    canAccept,
    canStartSas: phaseCode === VerificationPhase.Ready || (phaseCode === VerificationPhase.Started && !!request?.verifier),
    canConfirmSas: false,
    canShowQr: canShowQrFromRequest(request),
    canScanQr: canScanQrFromRequest(request),
    canConfirmQr: false,
    qrCodeBase64: "",
    ...extra,
  };
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

function emitVerification(extra = {}, message = "") {
  if (!currentVerificationRequest) {
    post("verification", {
      phaseCode: 0,
      phase: "None",
      source: "",
      canAccept: false,
      canStartSas: false,
      canConfirmSas: false,
      canShowQr: false,
      canScanQr: false,
      canConfirmQr: false,
      qrCodeBase64: "",
      ...extra,
    }, message);
    return;
  }
  post("verification", verificationPayloadFromRequest(currentVerificationRequest, currentVerificationSource, {
    qrCodeBase64: currentQrCodeBase64,
    ...extra,
  }), message);
}

function attachVerifier(verifier) {
  if (!verifier || currentVerifier === verifier) {
    return;
  }
  currentVerifier = verifier;
  verifier.on(VerifierEvent.ShowSas, (callbacks) => {
    currentSas = callbacks;
    emitVerification(sasPayloadFromCallbacks(callbacks), "Compare the SAS on both devices");
  });
  verifier.on(VerifierEvent.Cancel, (error) => {
    currentSas = null;
    currentQrReciprocate = null;
    currentQrCodeBase64 = "";
    const message = error?.message || "Verification cancelled";
    emitVerification({ canConfirmSas: false, canConfirmQr: false, qrCodeBase64: "" }, message);
  });
  verifier.on(VerifierEvent.ShowReciprocateQr, (callbacks) => {
    currentQrReciprocate = callbacks;
    emitVerification({ canConfirmQr: true }, "Confirm the other device scanned this QR code");
  });
  const callbacks = verifier.getShowSasCallbacks?.();
  if (callbacks) {
    currentSas = callbacks;
    emitVerification(sasPayloadFromCallbacks(callbacks), "Compare the SAS on both devices");
  }
  const qrCallbacks = verifier.getReciprocateQrCodeCallbacks?.();
  if (qrCallbacks) {
    currentQrReciprocate = qrCallbacks;
    emitVerification({ canConfirmQr: true }, "Confirm the other device scanned this QR code");
  }
}

function runVerifier(verifier) {
  if (!verifier || runningVerifiers.has(verifier)) {
    return;
  }
  runningVerifiers.add(verifier);
  attachVerifier(verifier);
  verifier.verify()
    .then(() => {
      currentSas = null;
      currentQrReciprocate = null;
      currentQrCodeBase64 = "";
      emitVerification({ canConfirmSas: false, canConfirmQr: false, qrCodeBase64: "" }, "Verification complete");
    })
    .catch((error) => {
      currentSas = null;
      currentQrReciprocate = null;
      currentQrCodeBase64 = "";
      const message = error?.message || "Verification cancelled";
      emitVerification({ canConfirmSas: false, canConfirmQr: false, qrCodeBase64: "" }, message);
    });
}

function trackVerificationRequest(request, source) {
  currentVerificationRequest = request;
  currentVerificationSource = source || "";
  currentSas = null;
  currentQrCodeBase64 = "";
  currentQrReciprocate = null;
  request.on(VerificationRequestEvent.Change, () => {
    if (request.verifier) {
      attachVerifier(request.verifier);
      if (request.chosenMethod === VerificationMethod.Reciprocate) {
        runVerifier(request.verifier);
      }
    }
    emitVerification();
  });
  if (request.verifier) {
    attachVerifier(request.verifier);
    if (request.chosenMethod === VerificationMethod.Reciprocate) {
      runVerifier(request.verifier);
    }
  }
  emitVerification({}, "Verification request ready");
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
  client.on(CryptoEvent.VerificationRequestReceived, (request) => {
    trackVerificationRequest(request, "incoming");
  });
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
  currentVerificationRequest = null;
  currentVerificationSource = "";
  currentVerifier = null;
  currentSas = null;
  currentQrCodeBase64 = "";
  currentQrReciprocate = null;
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
  const resolvedHomeserver = await resolveHomeserver(payload.homeserver);
  const homeserver = resolvedHomeserver.baseUrl;
  if (resolvedHomeserver.source !== "direct") {
    status(`Resolved ${resolvedHomeserver.input} to ${homeserver}`);
  }
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
  const resolvedHomeserver = await resolveHomeserver(payload.homeserver);
  const homeserver = resolvedHomeserver.baseUrl;
  if (resolvedHomeserver.source !== "direct") {
    status(`Resolved ${resolvedHomeserver.input} to ${homeserver}`);
  }
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

async function startOwnVerification() {
  if (!client) {
    throw new Error("Matrix runtime is not signed in.");
  }
  const crypto = client.getCrypto?.();
  if (!crypto?.requestOwnUserVerification) {
    throw new Error("Matrix crypto verification is not available.");
  }
  const request = await crypto.requestOwnUserVerification();
  trackVerificationRequest(request, "outgoing");
}

async function acceptVerification() {
  if (!currentVerificationRequest) {
    throw new Error("No active verification request.");
  }
  await currentVerificationRequest.accept();
  emitVerification({}, "Verification accepted");
}

async function startSasVerification() {
  if (!currentVerificationRequest) {
    throw new Error("No active verification request.");
  }
  const verifier = currentVerificationRequest.verifier
    || await currentVerificationRequest.startVerification(VerificationMethod.Sas);
  runVerifier(verifier);
  emitVerification({}, "SAS verification started");
}

async function generateQrVerification() {
  if (!currentVerificationRequest) {
    throw new Error("No active verification request.");
  }
  const bytes = await currentVerificationRequest.generateQRCode();
  if (!bytes || !bytes.length) {
    throw new Error("QR code verification is not available for the other device.");
  }
  currentQrCodeBase64 = bytesToBase64(bytes);
  emitVerification({
    qrCodeBase64: currentQrCodeBase64,
    canShowQr: false,
  }, "Show this QR code on the other device");
}

async function scanQrVerification(payload) {
  if (!currentVerificationRequest) {
    throw new Error("No active verification request.");
  }
  const bytes = base64ToBytes(payload.qrCodeBase64);
  if (!bytes.length) {
    throw new Error("No QR code payload was scanned.");
  }
  const verifier = await currentVerificationRequest.scanQRCode(bytes);
  currentQrCodeBase64 = "";
  runVerifier(verifier);
  emitVerification({ qrCodeBase64: "", canConfirmQr: false }, "QR code scanned");
}

async function confirmQrVerification() {
  if (!currentQrReciprocate) {
    throw new Error("No QR scan is waiting for confirmation.");
  }
  await currentQrReciprocate.confirm();
  currentQrReciprocate = null;
  currentQrCodeBase64 = "";
  emitVerification({ canConfirmQr: false, qrCodeBase64: "" }, "QR scan confirmed");
}

async function confirmSasVerification() {
  if (!currentSas) {
    throw new Error("No SAS is waiting for confirmation.");
  }
  await currentSas.confirm();
  currentSas = null;
  emitVerification({ canConfirmSas: false }, "SAS confirmed");
}

function mismatchSasVerification() {
  if (!currentSas) {
    throw new Error("No SAS is waiting for confirmation.");
  }
  currentSas.mismatch();
  currentSas = null;
  emitVerification({ canConfirmSas: false }, "SAS mismatch sent");
}

async function cancelVerification() {
  if (currentSas) {
    currentSas.cancel();
  } else if (currentQrReciprocate) {
    currentQrReciprocate.cancel();
  } else if (currentVerifier) {
    currentVerifier.cancel(new Error("User cancelled verification"));
  } else if (currentVerificationRequest) {
    await currentVerificationRequest.cancel();
  }
  currentSas = null;
  currentQrReciprocate = null;
  currentQrCodeBase64 = "";
  emitVerification({ canConfirmSas: false, canConfirmQr: false, qrCodeBase64: "" }, "Verification cancelled");
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
    } else if (command.op === "startOwnVerification") {
      await startOwnVerification();
    } else if (command.op === "acceptVerification") {
      await acceptVerification();
    } else if (command.op === "startSasVerification") {
      await startSasVerification();
    } else if (command.op === "generateQrVerification") {
      await generateQrVerification();
    } else if (command.op === "scanQrVerification") {
      await scanQrVerification(payload);
    } else if (command.op === "confirmQrVerification") {
      await confirmQrVerification();
    } else if (command.op === "confirmSasVerification") {
      await confirmSasVerification();
    } else if (command.op === "mismatchSasVerification") {
      mismatchSasVerification();
    } else if (command.op === "cancelVerification") {
      await cancelVerification();
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
