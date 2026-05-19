package io.github.cgissing.matrixrich;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NativeRoomListItemTest {
    @Test
    public void usesTypingSummaryAsPreviewWhenPresent() {
        NativeRoom room = new NativeRoom("!room:example.org", "Research", "Last message", "Alice is typing...", "R", 3);

        NativeRoomListItem item = NativeRoomListItem.from(room, true);

        assertEquals("Research", item.title);
        assertEquals("Alice is typing...", item.preview);
        assertTrue(item.typing);
        assertTrue(item.selected);
        assertEquals("3", item.unreadLabel);
    }

    @Test
    public void fallsBackToTitleInitialWhenInitialsAreMissing() {
        NativeRoom room = new NativeRoom("!room:example.org", "Matrix Room", "Quiet room", "", 0);

        NativeRoomListItem item = NativeRoomListItem.from(room, false);

        assertEquals("M", item.initials);
        assertEquals("Quiet room", item.preview);
        assertFalse(item.typing);
        assertFalse(item.hasUnread());
    }

    @Test
    public void capsLargeUnreadCounts() {
        NativeRoom room = new NativeRoom("!room:example.org", "Busy", "Many messages", "B", 120);

        NativeRoomListItem item = NativeRoomListItem.from(room, false);

        assertEquals("99+", item.unreadLabel);
        assertTrue(item.hasUnread());
    }
}
