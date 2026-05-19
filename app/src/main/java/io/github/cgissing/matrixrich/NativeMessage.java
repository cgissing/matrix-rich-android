package io.github.cgissing.matrixrich;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NativeMessage {
    public final String eventId;
    public final String sender;
    public final String time;
    public final String bodyMarkdown;
    public final boolean outbound;
    public final List<NativeReaction> reactions;

    public NativeMessage(String sender, String time, String bodyMarkdown, boolean outbound) {
        this("", sender, time, bodyMarkdown, outbound, Collections.emptyList());
    }

    public NativeMessage(String eventId, String sender, String time, String bodyMarkdown, boolean outbound, List<NativeReaction> reactions) {
        this.eventId = clean(eventId);
        this.sender = sender;
        this.time = time;
        this.bodyMarkdown = bodyMarkdown;
        this.outbound = outbound;
        this.reactions = Collections.unmodifiableList(new ArrayList<>(reactions == null ? Collections.emptyList() : reactions));
    }

    private static String clean(String value) {
        return value == null ? "" : value;
    }
}
