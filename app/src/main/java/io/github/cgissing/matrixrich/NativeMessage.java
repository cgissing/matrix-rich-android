package io.github.cgissing.matrixrich;

public final class NativeMessage {
    public final String sender;
    public final String time;
    public final String bodyMarkdown;
    public final boolean outbound;

    public NativeMessage(String sender, String time, String bodyMarkdown, boolean outbound) {
        this.sender = sender;
        this.time = time;
        this.bodyMarkdown = bodyMarkdown;
        this.outbound = outbound;
    }
}
