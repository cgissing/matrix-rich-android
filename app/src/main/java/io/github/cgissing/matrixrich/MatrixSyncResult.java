package io.github.cgissing.matrixrich;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MatrixSyncResult {
    public final List<NativeRoom> rooms;
    public final String nextBatch;
    private final Map<String, List<NativeMessage>> messagesByRoom;

    public MatrixSyncResult(List<NativeRoom> rooms, Map<String, List<NativeMessage>> messagesByRoom, String nextBatch) {
        this.rooms = Collections.unmodifiableList(new ArrayList<>(rooms));
        this.messagesByRoom = new HashMap<>(messagesByRoom);
        this.nextBatch = nextBatch == null ? "" : nextBatch;
    }

    public List<NativeMessage> messagesFor(String roomId) {
        List<NativeMessage> messages = messagesByRoom.get(roomId);
        if (messages == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(messages);
    }
}
