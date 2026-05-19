package io.github.cgissing.matrixrich;

public final class NativeRoom {
    public final String id;
    public final String title;
    public final String subtitle;
    public final String typingSummary;
    public final String initials;
    public final int unreadCount;

    public NativeRoom(String id, String title, String subtitle, String initials, int unreadCount) {
        this(id, title, subtitle, "", initials, unreadCount);
    }

    public NativeRoom(String id, String title, String subtitle, String typingSummary, String initials, int unreadCount) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.typingSummary = typingSummary == null ? "" : typingSummary;
        this.initials = initials;
        this.unreadCount = unreadCount;
    }
}
