package io.github.cgissing.matrixrich;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DemoMatrixState {
    private static final String RICH_MESSAGE =
            "Here is the derivation table from today's note.\n\n"
                    + "| Step | Result |\n"
                    + "| --- | --- |\n"
                    + "| Energy | $E = mc^2$ |\n"
                    + "| Integral | $\\int_0^1 x^2 dx$ |\n\n"
                    + "$$\\int_0^1 x^2\\,dx = \\frac{1}{3}$$";

    private DemoMatrixState() {
    }

    public static List<NativeRoom> rooms() {
        List<NativeRoom> rooms = new ArrayList<>();
        rooms.add(new NativeRoom("agent", "Agent DM", "Rich Markdown, tables and formulas", "A", 2));
        rooms.add(new NativeRoom("lab", "Research Lab", "Native Android message layout", "R", 0));
        rooms.add(new NativeRoom("ops", "Push Bridge", "ntfy wake path", "P", 1));
        return Collections.unmodifiableList(rooms);
    }

    public static List<NativeMessage> messagesFor(String roomId) {
        List<NativeMessage> messages = new ArrayList<>();
        if ("lab".equals(roomId)) {
            messages.add(new NativeMessage("Hermes", "09:21", "The experiment log is ready. I kept the table and formula in the same message.", false));
            messages.add(new NativeMessage("You", "09:22", "Send it here and keep the layout compact.", true));
        } else if ("ops".equals(roomId)) {
            messages.add(new NativeMessage("ntfy", "08:10", "Foreground listener is configured from the Push tab.", false));
            messages.add(new NativeMessage("Hermes", "08:11", "A `Click: matrixrich://open?...` payload wakes this native client.", false));
        } else {
            messages.add(new NativeMessage("Hermes", "10:02", RICH_MESSAGE, false));
            messages.add(new NativeMessage("You", "10:04", "Looks readable on a phone screen.", true));
        }
        return Collections.unmodifiableList(messages);
    }

    public static String richMessageFixture() {
        return RICH_MESSAGE;
    }
}
