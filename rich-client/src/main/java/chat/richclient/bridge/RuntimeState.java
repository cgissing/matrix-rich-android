package chat.richclient.bridge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

public final class RuntimeState {
    public final boolean runtimeReady;
    public final String runtimeName;
    public final String runtimeError;
    public final boolean loggedIn;
    public final String userId;
    public final String syncState;
    public final String syncError;
    public final List<RoomSummary> rooms;
    public final String selectedRoomId;
    public final Map<String, List<TimelineEvent>> timelines;
    public final Map<String, List<String>> typingByRoom;
    public final Map<String, List<ReactionSummary>> reactionsByEvent;
    public final List<VerificationSummary> verifications;
    public final boolean pushRegistered;
    public final String pushStatus;
    public final String pushEndpoint;
    public final String cryptoState;
    public final String cryptoStatus;
    public final String cryptoSecretName;
    public final boolean cryptoRecoveryRequired;

    public RuntimeState() {
        this(
                false,
                "",
                "",
                false,
                "",
                "idle",
                "",
                Collections.emptyList(),
                "",
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyList(),
                false,
                "not registered",
                "",
                "idle",
                "idle",
                "",
                false);
    }

    private RuntimeState(
            boolean runtimeReady,
            String runtimeName,
            String runtimeError,
            boolean loggedIn,
            String userId,
            String syncState,
            String syncError,
            List<RoomSummary> rooms,
            String selectedRoomId,
            Map<String, List<TimelineEvent>> timelines,
            Map<String, List<String>> typingByRoom,
            Map<String, List<ReactionSummary>> reactionsByEvent,
            List<VerificationSummary> verifications,
            boolean pushRegistered,
            String pushStatus,
            String pushEndpoint,
            String cryptoState,
            String cryptoStatus,
            String cryptoSecretName,
            boolean cryptoRecoveryRequired) {
        this.runtimeReady = runtimeReady;
        this.runtimeName = runtimeName;
        this.runtimeError = runtimeError;
        this.loggedIn = loggedIn;
        this.userId = userId;
        this.syncState = syncState;
        this.syncError = syncError;
        this.rooms = Collections.unmodifiableList(new ArrayList<>(rooms));
        this.selectedRoomId = selectedRoomId;
        this.timelines = immutableListMap(timelines);
        this.typingByRoom = immutableListMap(typingByRoom);
        this.reactionsByEvent = immutableListMap(reactionsByEvent);
        this.verifications = Collections.unmodifiableList(new ArrayList<>(verifications));
        this.pushRegistered = pushRegistered;
        this.pushStatus = pushStatus;
        this.pushEndpoint = pushEndpoint;
        this.cryptoState = cryptoState;
        this.cryptoStatus = cryptoStatus;
        this.cryptoSecretName = cryptoSecretName;
        this.cryptoRecoveryRequired = cryptoRecoveryRequired;
    }

    public RuntimeState reduce(BridgeEvent event) {
        if ("runtime.ready".equals(event.type)) {
            return copy()
                    .runtimeReady(true)
                    .runtimeName(event.payload.optString("runtime", runtimeName))
                    .runtimeError("")
                    .build();
        }
        if ("runtime.error".equals(event.type)) {
            return copy()
                    .runtimeError(event.payload.optString("message", runtimeError))
                    .build();
        }
        if ("auth.state".equals(event.type)) {
            return copy()
                    .loggedIn(event.payload.optBoolean("loggedIn", loggedIn))
                    .userId(event.payload.optString("userId", userId))
                    .build();
        }
        if ("sync.state".equals(event.type)) {
            return copy()
                    .syncState(event.payload.optString("state", syncState))
                    .syncError(event.payload.optString("error", ""))
                    .build();
        }
        if ("crypto.state".equals(event.type)) {
            String nextCryptoState = event.payload.optString("state", cryptoState);
            return copy()
                    .cryptoState(nextCryptoState)
                    .cryptoStatus(event.payload.optString("status", nextCryptoState))
                    .cryptoSecretName(event.payload.optString("secretName", cryptoSecretName))
                    .cryptoRecoveryRequired(event.payload.optBoolean(
                            "recoveryRequired",
                            "recovery_required".equals(nextCryptoState)))
                    .build();
        }
        if ("rooms.snapshot".equals(event.type)) {
            List<RoomSummary> nextRooms = parseRooms(event.payload.optJSONArray("rooms"));
            String nextSelectedRoomId = selectedRoomId;
            if (nextSelectedRoomId.isEmpty() && !nextRooms.isEmpty()) {
                nextSelectedRoomId = nextRooms.get(0).roomId;
            }
            return copy()
                    .rooms(nextRooms)
                    .selectedRoomId(nextSelectedRoomId)
                    .build();
        }
        if ("rooms.open".equals(event.type)) {
            return selectRoom(event.payload.optString("roomId", selectedRoomId));
        }
        if ("timeline.snapshot".equals(event.type)) {
            String roomId = event.payload.optString("roomId", selectedRoomId);
            Map<String, List<TimelineEvent>> nextTimelines = mutableCopy(timelines);
            nextTimelines.put(roomId, parseTimeline(event.payload.optJSONArray("events")));
            return copy()
                    .timelines(nextTimelines)
                    .selectedRoomId(roomId.isEmpty() ? selectedRoomId : roomId)
                    .build();
        }
        if ("timeline.append".equals(event.type)) {
            String roomId = event.payload.optString("roomId", selectedRoomId);
            TimelineEvent timelineEvent = TimelineEvent.fromJson(event.payload.optJSONObject("event"));
            Map<String, List<TimelineEvent>> nextTimelines = mutableCopy(timelines);
            List<TimelineEvent> events = new ArrayList<>(timelineForRoom(roomId));
            events.add(timelineEvent);
            nextTimelines.put(roomId, events);
            return copy()
                    .timelines(nextTimelines)
                    .selectedRoomId(roomId.isEmpty() ? selectedRoomId : roomId)
                    .build();
        }
        if ("typing.update".equals(event.type)) {
            String roomId = event.payload.optString("roomId", selectedRoomId);
            Map<String, List<String>> nextTyping = mutableCopy(typingByRoom);
            nextTyping.put(roomId, parseStrings(event.payload.optJSONArray("users")));
            return copy().typingByRoom(nextTyping).build();
        }
        if ("reactions.update".equals(event.type)) {
            String eventId = event.payload.optString("eventId", "");
            Map<String, List<ReactionSummary>> nextReactions = mutableCopy(reactionsByEvent);
            nextReactions.put(eventId, parseReactions(event.payload.optJSONArray("reactions")));
            return copy().reactionsByEvent(nextReactions).build();
        }
        if ("verification.update".equals(event.type)) {
            List<VerificationSummary> nextVerifications = new ArrayList<>(verifications);
            nextVerifications.add(VerificationSummary.fromJson(event.payload));
            return copy().verifications(nextVerifications).build();
        }
        if ("push.registrationState".equals(event.type)) {
            return copy()
                    .pushRegistered(event.payload.optBoolean("registered", pushRegistered))
                    .pushStatus(event.payload.optString("status", pushStatus))
                    .pushEndpoint(event.payload.optString("endpoint", pushEndpoint))
                    .build();
        }
        return this;
    }

    public RuntimeState selectRoom(String roomId) {
        if (roomId == null || roomId.isEmpty()) {
            return this;
        }
        return copy().selectedRoomId(roomId).build();
    }

    public List<TimelineEvent> timelineForSelectedRoom() {
        return timelineForRoom(selectedRoomId);
    }

    public List<TimelineEvent> timelineForRoom(String roomId) {
        List<TimelineEvent> events = timelines.get(roomId);
        return events == null ? Collections.emptyList() : events;
    }

    public List<String> typingUsersForRoom(String roomId) {
        List<String> users = typingByRoom.get(roomId);
        return users == null ? Collections.emptyList() : users;
    }

    public List<ReactionSummary> reactionsForEvent(String eventId) {
        List<ReactionSummary> reactions = reactionsByEvent.get(eventId);
        return reactions == null ? Collections.emptyList() : reactions;
    }

    public String typingSummaryForSelectedRoom() {
        List<String> users = typingUsersForRoom(selectedRoomId);
        if (users.isEmpty()) {
            return "";
        }
        if (users.size() == 1) {
            return users.get(0) + " is typing";
        }
        return users.size() + " people are typing";
    }

    private Builder copy() {
        return new Builder(this);
    }

    private static List<RoomSummary> parseRooms(JSONArray array) {
        List<RoomSummary> rooms = new ArrayList<>();
        if (array == null) {
            return rooms;
        }
        for (int i = 0; i < array.length(); i++) {
            rooms.add(RoomSummary.fromJson(array.optJSONObject(i)));
        }
        return rooms;
    }

    private static List<TimelineEvent> parseTimeline(JSONArray array) {
        List<TimelineEvent> events = new ArrayList<>();
        if (array == null) {
            return events;
        }
        for (int i = 0; i < array.length(); i++) {
            events.add(TimelineEvent.fromJson(array.optJSONObject(i)));
        }
        return events;
    }

    private static List<ReactionSummary> parseReactions(JSONArray array) {
        List<ReactionSummary> reactions = new ArrayList<>();
        if (array == null) {
            return reactions;
        }
        for (int i = 0; i < array.length(); i++) {
            reactions.add(ReactionSummary.fromJson(array.optJSONObject(i)));
        }
        return reactions;
    }

    private static List<String> parseStrings(JSONArray array) {
        List<String> values = new ArrayList<>();
        if (array == null) {
            return values;
        }
        for (int i = 0; i < array.length(); i++) {
            values.add(array.optString(i));
        }
        return values;
    }

    private static <T> Map<String, List<T>> mutableCopy(Map<String, List<T>> source) {
        Map<String, List<T>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<T>> entry : source.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return copy;
    }

    private static <T> Map<String, List<T>> immutableListMap(Map<String, List<T>> source) {
        Map<String, List<T>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<T>> entry : source.entrySet()) {
            copy.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
        }
        return Collections.unmodifiableMap(copy);
    }

    public static final class RoomSummary {
        public final String roomId;
        public final String name;
        public final String lastMessage;
        public final int unreadCount;
        public final boolean encrypted;

        private RoomSummary(String roomId, String name, String lastMessage, int unreadCount, boolean encrypted) {
            this.roomId = roomId;
            this.name = name;
            this.lastMessage = lastMessage;
            this.unreadCount = unreadCount;
            this.encrypted = encrypted;
        }

        static RoomSummary fromJson(JSONObject json) {
            JSONObject value = json == null ? new JSONObject() : json;
            return new RoomSummary(
                    value.optString("roomId", ""),
                    value.optString("name", value.optString("roomId", "")),
                    value.optString("lastMessage", ""),
                    value.optInt("unreadCount", 0),
                    value.optBoolean("encrypted", false));
        }
    }

    public static final class TimelineEvent {
        public final String eventId;
        public final String sender;
        public final String body;
        public final long timestamp;
        public final boolean outgoing;

        private TimelineEvent(String eventId, String sender, String body, long timestamp, boolean outgoing) {
            this.eventId = eventId;
            this.sender = sender;
            this.body = body;
            this.timestamp = timestamp;
            this.outgoing = outgoing;
        }

        static TimelineEvent fromJson(JSONObject json) {
            JSONObject value = json == null ? new JSONObject() : json;
            return new TimelineEvent(
                    value.optString("eventId", ""),
                    value.optString("sender", ""),
                    value.optString("body", ""),
                    value.optLong("timestamp", 0L),
                    value.optBoolean("outgoing", false));
        }
    }

    public static final class ReactionSummary {
        public final String key;
        public final int count;
        public final boolean selected;

        private ReactionSummary(String key, int count, boolean selected) {
            this.key = key;
            this.count = count;
            this.selected = selected;
        }

        static ReactionSummary fromJson(JSONObject json) {
            JSONObject value = json == null ? new JSONObject() : json;
            return new ReactionSummary(
                    value.optString("key", ""),
                    value.optInt("count", 0),
                    value.optBoolean("selected", false));
        }
    }

    public static final class VerificationSummary {
        public final String transactionId;
        public final String userId;
        public final String state;

        private VerificationSummary(String transactionId, String userId, String state) {
            this.transactionId = transactionId;
            this.userId = userId;
            this.state = state;
        }

        static VerificationSummary fromJson(JSONObject json) {
            JSONObject value = json == null ? new JSONObject() : json;
            return new VerificationSummary(
                    value.optString("transactionId", ""),
                    value.optString("userId", ""),
                    value.optString("state", ""));
        }
    }

    private static final class Builder {
        private boolean runtimeReady;
        private String runtimeName;
        private String runtimeError;
        private boolean loggedIn;
        private String userId;
        private String syncState;
        private String syncError;
        private List<RoomSummary> rooms;
        private String selectedRoomId;
        private Map<String, List<TimelineEvent>> timelines;
        private Map<String, List<String>> typingByRoom;
        private Map<String, List<ReactionSummary>> reactionsByEvent;
        private List<VerificationSummary> verifications;
        private boolean pushRegistered;
        private String pushStatus;
        private String pushEndpoint;
        private String cryptoState;
        private String cryptoStatus;
        private String cryptoSecretName;
        private boolean cryptoRecoveryRequired;

        private Builder(RuntimeState state) {
            runtimeReady = state.runtimeReady;
            runtimeName = state.runtimeName;
            runtimeError = state.runtimeError;
            loggedIn = state.loggedIn;
            userId = state.userId;
            syncState = state.syncState;
            syncError = state.syncError;
            rooms = state.rooms;
            selectedRoomId = state.selectedRoomId;
            timelines = state.timelines;
            typingByRoom = state.typingByRoom;
            reactionsByEvent = state.reactionsByEvent;
            verifications = state.verifications;
            pushRegistered = state.pushRegistered;
            pushStatus = state.pushStatus;
            pushEndpoint = state.pushEndpoint;
            cryptoState = state.cryptoState;
            cryptoStatus = state.cryptoStatus;
            cryptoSecretName = state.cryptoSecretName;
            cryptoRecoveryRequired = state.cryptoRecoveryRequired;
        }

        Builder runtimeReady(boolean value) {
            runtimeReady = value;
            return this;
        }

        Builder runtimeName(String value) {
            runtimeName = value;
            return this;
        }

        Builder runtimeError(String value) {
            runtimeError = value;
            return this;
        }

        Builder loggedIn(boolean value) {
            loggedIn = value;
            return this;
        }

        Builder userId(String value) {
            userId = value;
            return this;
        }

        Builder syncState(String value) {
            syncState = value;
            return this;
        }

        Builder syncError(String value) {
            syncError = value;
            return this;
        }

        Builder rooms(List<RoomSummary> value) {
            rooms = value;
            return this;
        }

        Builder selectedRoomId(String value) {
            selectedRoomId = value;
            return this;
        }

        Builder timelines(Map<String, List<TimelineEvent>> value) {
            timelines = value;
            return this;
        }

        Builder typingByRoom(Map<String, List<String>> value) {
            typingByRoom = value;
            return this;
        }

        Builder reactionsByEvent(Map<String, List<ReactionSummary>> value) {
            reactionsByEvent = value;
            return this;
        }

        Builder verifications(List<VerificationSummary> value) {
            verifications = value;
            return this;
        }

        Builder pushRegistered(boolean value) {
            pushRegistered = value;
            return this;
        }

        Builder pushStatus(String value) {
            pushStatus = value;
            return this;
        }

        Builder pushEndpoint(String value) {
            pushEndpoint = value;
            return this;
        }

        Builder cryptoState(String value) {
            cryptoState = value;
            return this;
        }

        Builder cryptoStatus(String value) {
            cryptoStatus = value;
            return this;
        }

        Builder cryptoSecretName(String value) {
            cryptoSecretName = value;
            return this;
        }

        Builder cryptoRecoveryRequired(boolean value) {
            cryptoRecoveryRequired = value;
            return this;
        }

        RuntimeState build() {
            return new RuntimeState(
                    runtimeReady,
                    runtimeName,
                    runtimeError,
                    loggedIn,
                    userId,
                    syncState,
                    syncError,
                    rooms,
                    selectedRoomId,
                    timelines,
                    typingByRoom,
                    reactionsByEvent,
                    verifications,
                    pushRegistered,
                    pushStatus,
                    pushEndpoint,
                    cryptoState,
                    cryptoStatus,
                    cryptoSecretName,
                    cryptoRecoveryRequired);
        }
    }
}
