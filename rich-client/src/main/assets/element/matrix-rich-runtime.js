(function () {
    "use strict";

    if (typeof Promise.withResolvers !== "function") {
        Object.defineProperty(Promise, "withResolvers", {
            configurable: true,
            writable: true,
            value: function () {
                var resolve;
                var reject;
                var promise = new Promise(function (promiseResolve, promiseReject) {
                    resolve = promiseResolve;
                    reject = promiseReject;
                });
                return {
                    promise: promise,
                    resolve: resolve,
                    reject: reject
                };
            }
        });
    }

    var state = {
        client: null,
        currentRoomId: "",
        matrix: null,
        require: null,
        started: false
    };

    var COMMANDS = [
        "auth.loginPassword",
        "auth.logout",
        "rooms.subscribe",
        "rooms.open",
        "timeline.paginateBack",
        "messages.sendText",
        "typing.set",
        "reactions.send",
        "verification.action",
        "push.register",
        "sync.once"
    ];

    var EVENTS = [
        "runtime.ready",
        "runtime.error",
        "auth.state",
        "sync.state",
        "rooms.snapshot",
        "timeline.snapshot",
        "timeline.append",
        "typing.update",
        "reactions.update",
        "verification.update",
        "push.registrationState"
    ];

    function postEvent(type, payload) {
        var event = { type: type, payload: payload || {} };
        if (window.MatrixRichBridge && typeof window.MatrixRichBridge.postEvent === "function") {
            window.MatrixRichBridge.postEvent(JSON.stringify(event));
        }
    }

    function postError(message, error) {
        postEvent("runtime.error", {
            message: message,
            detail: error && (error.stack || error.message || String(error))
        });
    }

    function describeError(error) {
        if (!error) {
            return "unknown error";
        }
        if (error.data && error.data.error) {
            return text(error.data.error);
        }
        if (error.errcode && error.message) {
            return text(error.errcode) + ": " + text(error.message);
        }
        return text(error.message || error);
    }

    function describeCommandFailure(command, error) {
        var type = command && command.type ? command.type : "unknown";
        var detail = describeError(error);
        if (type === "auth.loginPassword") {
            postEvent("auth.state", { loggedIn: false, userId: "", status: "login_failed" });
            postEvent("sync.state", { state: "login_failed", error: detail });
            return "Login failed: " + detail;
        }
        return "Bridge command failed: " + type + ": " + detail;
    }

    function text(value) {
        return value == null ? "" : String(value);
    }

    function normalizeHomeserver(url) {
        var value = text(url).trim();
        if (!value) {
            throw new Error("Homeserver URL is required");
        }
        if (value.indexOf("://") < 0) {
            value = "https://" + value;
        }
        return value.replace(/\/+$/, "");
    }

    function getMatrixRequire() {
        if (state.require) {
            return Promise.resolve(state.require);
        }
        return new Promise(function (resolve, reject) {
            var attempts = 0;
            var timer = setInterval(function () {
                attempts += 1;
                if (state.require) {
                    clearInterval(timer);
                    resolve(state.require);
                } else if (attempts > 80) {
                    clearInterval(timer);
                    reject(new Error("Element Web webpack runtime was not captured"));
                }
            }, 125);
        });
    }

    async function ensureMatrix() {
        if (state.matrix) {
            return state.matrix;
        }
        var req = await getMatrixRequire();
        if (typeof req.e === "function") {
            try {
                await Promise.all([req.e(9544), req.e(2702)]);
            } catch (error) {
                postError("Unable to load Element Web runtime chunks", error);
            }
        }
        state.matrix = req("../../node_modules/matrix-js-sdk/src/matrix.ts");
        postEvent("runtime.ready", {
            runtime: "element-web-v1.12.17-matrix-js-sdk",
            commands: COMMANDS,
            events: EVENTS
        });
        return state.matrix;
    }

    function attachClient(client) {
        var matrix = state.matrix || {};
        client.on((matrix.ClientEvent && matrix.ClientEvent.Sync) || "sync", function (syncState, oldState, data) {
            postEvent("sync.state", {
                state: text(syncState),
                error: data && data.error ? text(data.error.message || data.error) : ""
            });
            emitRoomsSnapshot();
            if (state.currentRoomId) {
                emitTimelineSnapshot(state.currentRoomId);
            }
        });
        client.on((matrix.RoomEvent && matrix.RoomEvent.Timeline) || "Room.timeline", function (event, room, toStartOfTimeline) {
            if (!room || toStartOfTimeline) {
                return;
            }
            postEvent("timeline.append", {
                roomId: room.roomId,
                event: serializeEvent(event, room)
            });
            emitReactionsForRoom(room);
            emitRoomsSnapshot();
        });
        client.on((matrix.RoomMemberEvent && matrix.RoomMemberEvent.Typing) || "RoomMember.typing", function (event, member) {
            var roomId = member && member.roomId ? member.roomId : state.currentRoomId;
            emitTyping(roomId);
        });
    }

    async function loginPassword(payload) {
        var matrix = await ensureMatrix();
        var homeserver = normalizeHomeserver(payload.homeserver);
        var username = text(payload.username).trim();
        var password = text(payload.password);
        if (!username || !password) {
            throw new Error("Username and password are required");
        }

        postEvent("auth.state", { loggedIn: false, userId: "", status: "logging_in" });
        postEvent("sync.state", { state: "login" });

        var loginClient = matrix.createClient({
            baseUrl: homeserver,
            timelineSupport: true,
            useAuthorizationHeader: true
        });
        var loginPayload = {
            type: "m.login.password",
            identifier: { type: "m.id.user", user: username },
            password: password,
            initial_device_display_name: "Matrix Rich Android"
        };
        var result = await loginClient.loginRequest(loginPayload);

        var client = matrix.createClient({
            baseUrl: homeserver,
            accessToken: result.access_token,
            userId: result.user_id,
            deviceId: result.device_id,
            timelineSupport: true,
            useAuthorizationHeader: true
        });
        state.client = client;
        attachClient(client);
        if (typeof client.initRustCrypto === "function") {
            try {
                await client.initRustCrypto();
            } catch (error) {
                postError("E2EE crypto initialization failed", error);
            }
        }
        state.started = true;
        await client.startClient({
            initialSyncLimit: 30,
            lazyLoadMembers: true
        });
        postEvent("auth.state", {
            loggedIn: true,
            userId: result.user_id,
            deviceId: result.device_id,
            homeserver: homeserver
        });
    }

    async function logout() {
        if (!state.client) {
            return;
        }
        var client = state.client;
        state.client = null;
        state.started = false;
        try {
            client.stopClient();
            await client.logout();
        } finally {
            postEvent("auth.state", { loggedIn: false, userId: "" });
            postEvent("rooms.snapshot", { rooms: [] });
        }
    }

    function requireClient() {
        if (!state.client) {
            throw new Error("Matrix runtime is not logged in");
        }
        return state.client;
    }

    function emitRoomsSnapshot() {
        var client = state.client;
        if (!client) {
            return;
        }
        var rooms = client.getRooms().map(function (room) {
            return serializeRoom(client, room);
        });
        postEvent("rooms.snapshot", { rooms: rooms });
    }

    function serializeRoom(client, room) {
        var last = "";
        var events = room.getLiveTimeline().getEvents();
        for (var i = events.length - 1; i >= 0; i -= 1) {
            last = serializeEvent(events[i], room).body;
            if (last) {
                break;
            }
        }
        var unread = 0;
        if (typeof room.getUnreadNotificationCount === "function") {
            unread = room.getUnreadNotificationCount();
        }
        return {
            roomId: room.roomId,
            name: room.name || room.getDefaultRoomName(client.getUserId()),
            lastMessage: last,
            unreadCount: unread,
            encrypted: typeof client.isRoomEncrypted === "function" ? client.isRoomEncrypted(room.roomId) : false
        };
    }

    function emitTimelineSnapshot(roomId) {
        var client = requireClient();
        var room = client.getRoom(roomId);
        if (!room) {
            return;
        }
        state.currentRoomId = roomId;
        var events = room.getLiveTimeline().getEvents().map(function (event) {
            return serializeEvent(event, room);
        }).filter(function (event) {
            return event.body || event.eventId;
        });
        postEvent("timeline.snapshot", { roomId: roomId, events: events });
        emitTyping(roomId);
        emitReactionsForRoom(room);
    }

    function serializeEvent(event, room) {
        var content = getClearContent(event);
        var eventId = typeof event.getId === "function" ? event.getId() : text(event.getId);
        var sender = typeof event.getSender === "function" ? event.getSender() : text(event.sender);
        var body = content.body || content.formatted_body || "";
        if (!body && typeof event.isDecryptionFailure === "function" && event.isDecryptionFailure()) {
            body = "[Unable to decrypt]";
        }
        if (!body && content.msgtype && content.msgtype.indexOf("m.key.verification") === 0) {
            body = content.msgtype;
            postEvent("verification.update", {
                transactionId: content.transaction_id || eventId,
                userId: sender,
                state: content.msgtype
            });
        }
        return {
            eventId: eventId,
            sender: sender,
            body: body,
            timestamp: typeof event.getTs === "function" ? event.getTs() : 0,
            outgoing: state.client && sender === state.client.getUserId(),
            roomId: room && room.roomId ? room.roomId : ""
        };
    }

    function getClearContent(event) {
        if (event && typeof event.getClearContent === "function") {
            var clear = event.getClearContent();
            if (clear && Object.keys(clear).length) {
                return clear;
            }
        }
        if (event && typeof event.getContent === "function") {
            return event.getContent() || {};
        }
        return event && event.event && event.event.content ? event.event.content : {};
    }

    function emitTyping(roomId) {
        var client = state.client;
        var room = client && client.getRoom(roomId);
        if (!room || !room.currentState || typeof room.currentState.getMembers !== "function") {
            return;
        }
        var users = room.currentState.getMembers().filter(function (member) {
            return member.typing && member.userId !== client.getUserId();
        }).map(function (member) {
            return member.userId;
        });
        postEvent("typing.update", { roomId: roomId, users: users });
    }

    function emitReactionsForRoom(room) {
        var events = room.getLiveTimeline().getEvents();
        var grouped = {};
        events.forEach(function (event) {
            var content = getClearContent(event);
            var relates = content["m.relates_to"];
            if (!relates || relates.rel_type !== "m.annotation" || !relates.event_id) {
                return;
            }
            var eventId = relates.event_id;
            var key = relates.key || "";
            grouped[eventId] = grouped[eventId] || {};
            grouped[eventId][key] = grouped[eventId][key] || { key: key, count: 0, selected: false };
            grouped[eventId][key].count += 1;
            if (state.client && event.getSender && event.getSender() === state.client.getUserId()) {
                grouped[eventId][key].selected = true;
            }
        });
        Object.keys(grouped).forEach(function (eventId) {
            postEvent("reactions.update", {
                eventId: eventId,
                reactions: Object.keys(grouped[eventId]).map(function (key) {
                    return grouped[eventId][key];
                })
            });
        });
    }

    async function paginateBack(payload) {
        var client = requireClient();
        var roomId = payload.roomId || state.currentRoomId;
        var room = client.getRoom(roomId);
        if (!room) {
            return;
        }
        await client.scrollback(room, payload.limit || 30);
        emitTimelineSnapshot(roomId);
    }

    async function sendText(payload) {
        var client = requireClient();
        var roomId = payload.roomId || state.currentRoomId;
        var body = text(payload.body || payload.text);
        if (!roomId || !body.trim()) {
            return;
        }
        await client.sendTextMessage(roomId, body);
    }

    async function setTyping(payload) {
        var client = requireClient();
        var roomId = payload.roomId || state.currentRoomId;
        if (!roomId || typeof client.sendTyping !== "function") {
            return;
        }
        await client.sendTyping(roomId, Boolean(payload.typing), payload.timeout || 30000);
    }

    async function sendReaction(payload) {
        var client = requireClient();
        var roomId = payload.roomId || state.currentRoomId;
        if (!roomId || !payload.eventId || !payload.key) {
            return;
        }
        await client.sendEvent(roomId, "m.reaction", {
            "m.relates_to": {
                rel_type: "m.annotation",
                event_id: payload.eventId,
                key: payload.key
            }
        });
    }

    async function registerPush(payload) {
        var client = requireClient();
        var endpoint = text(payload.endpoint);
        var gateway = text(payload.gateway || payload.endpoint);
        if (!endpoint) {
            postEvent("push.registrationState", {
                registered: false,
                status: "UnifiedPush endpoint missing"
            });
            return;
        }
        await client.setPusher({
            kind: "http",
            app_id: payload.appId || "chat.richclient.unifiedpush",
            app_display_name: "Matrix Rich",
            device_display_name: "Android",
            pushkey: endpoint,
            lang: "en",
            data: {
                url: gateway,
                format: "event_id_only"
            }
        });
        postEvent("push.registrationState", {
            registered: true,
            status: "registered",
            endpoint: endpoint
        });
    }

    function syncOnce() {
        if (state.client) {
            emitRoomsSnapshot();
            if (state.currentRoomId) {
                emitTimelineSnapshot(state.currentRoomId);
            }
        }
    }

    async function handleCommand(command) {
        var payload = command.payload || {};
        switch (command.type) {
            case "auth.loginPassword":
                await loginPassword(payload);
                break;
            case "auth.logout":
                await logout();
                break;
            case "rooms.subscribe":
                emitRoomsSnapshot();
                break;
            case "rooms.open":
                state.currentRoomId = text(payload.roomId);
                emitTimelineSnapshot(state.currentRoomId);
                break;
            case "timeline.paginateBack":
                await paginateBack(payload);
                break;
            case "messages.sendText":
                await sendText(payload);
                break;
            case "typing.set":
                await setTyping(payload);
                break;
            case "reactions.send":
                await sendReaction(payload);
                break;
            case "verification.action":
                postEvent("verification.update", {
                    transactionId: text(payload.transactionId),
                    userId: text(payload.userId),
                    state: "native action requested: " + text(payload.action)
                });
                break;
            case "push.register":
                await registerPush(payload);
                break;
            case "sync.once":
                syncOnce();
                break;
            default:
                throw new Error("Unsupported bridge command: " + command.type);
        }
    }

    window.MatrixRichRuntime = {
        receive: function (commandJson) {
            var command;
            Promise.resolve()
                .then(function () {
                    command = JSON.parse(commandJson);
                    return command;
                })
                .then(handleCommand)
                .catch(function (error) {
                    postError(describeCommandFailure(command, error), error);
                });
        }
    };

    document.cookie = "element_mobile_redirect_to_guide=false; path=/; max-age=31536000";
    window.webpackChunkelement_web = window.webpackChunkelement_web || [];
    window.webpackChunkelement_web.push([[990001], {}, function (require) {
        state.require = require;
        postEvent("runtime.ready", {
            runtime: "element-web-webpack-captured",
            commands: COMMANDS,
            events: EVENTS
        });
        ensureMatrix().catch(function (error) {
            postError("Unable to initialize matrix-js-sdk runtime", error);
        });
    }]);

    postEvent("runtime.ready", {
        runtime: "bridge-preload",
        commands: COMMANDS,
        events: EVENTS
    });
}());
