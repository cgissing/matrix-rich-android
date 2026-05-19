package chat.richclient.bridge;

public final class RuntimeState {
    public final boolean runtimeReady;
    public final String runtimeName;
    public final boolean loggedIn;
    public final String userId;

    public RuntimeState() {
        this(false, "", false, "");
    }

    private RuntimeState(boolean runtimeReady, String runtimeName, boolean loggedIn, String userId) {
        this.runtimeReady = runtimeReady;
        this.runtimeName = runtimeName;
        this.loggedIn = loggedIn;
        this.userId = userId;
    }

    public RuntimeState reduce(BridgeEvent event) {
        if ("runtime.ready".equals(event.type)) {
            return new RuntimeState(
                    true,
                    event.payload.optString("runtime", runtimeName),
                    loggedIn,
                    userId);
        }
        if ("auth.state".equals(event.type)) {
            return new RuntimeState(
                    runtimeReady,
                    runtimeName,
                    event.payload.optBoolean("loggedIn", loggedIn),
                    event.payload.optString("userId", userId));
        }
        return this;
    }
}
