package io.github.cgissing.matrixrich;

public final class NativeRoomListItem {
    public final String title;
    public final String preview;
    public final String initials;
    public final String unreadLabel;
    public final boolean typing;
    public final boolean selected;

    private NativeRoomListItem(String title, String preview, String initials, String unreadLabel, boolean typing, boolean selected) {
        this.title = clean(title).isEmpty() ? "Unnamed room" : clean(title);
        this.preview = clean(preview);
        this.initials = normalizeInitials(initials, this.title);
        this.unreadLabel = unreadLabel;
        this.typing = typing;
        this.selected = selected;
    }

    public static NativeRoomListItem from(NativeRoom room, boolean selected) {
        String typingSummary = clean(room.typingSummary);
        boolean hasTyping = !typingSummary.isEmpty();
        String preview = hasTyping ? typingSummary : clean(room.subtitle);
        String unreadLabel = unreadLabel(room.unreadCount);
        return new NativeRoomListItem(room.title, preview, room.initials, unreadLabel, hasTyping, selected);
    }

    public boolean hasUnread() {
        return !unreadLabel.isEmpty();
    }

    private static String unreadLabel(int unreadCount) {
        if (unreadCount <= 0) {
            return "";
        }
        return unreadCount > 99 ? "99+" : String.valueOf(unreadCount);
    }

    private static String normalizeInitials(String initials, String title) {
        String value = clean(initials).trim();
        if (value.isEmpty()) {
            value = clean(title).trim();
        }
        if (value.isEmpty()) {
            return "?";
        }
        String first = value.substring(0, 1);
        return first.toUpperCase();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
