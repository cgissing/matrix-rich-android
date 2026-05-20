package chat.richclient.bridge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class RuntimeStateTest {
    @Test
    public void recordsRuntimeReady() {
        RuntimeState state = new RuntimeState();
        RuntimeState next = state.reduce(BridgeEvent.fromJson("{\"type\":\"runtime.ready\",\"payload\":{\"runtime\":\"element-web\"}}"));

        assertTrue(next.runtimeReady);
        assertEquals("element-web", next.runtimeName);
    }

    @Test
    public void recordsAuthState() {
        RuntimeState state = new RuntimeState();
        RuntimeState next = state.reduce(BridgeEvent.fromJson("{\"type\":\"auth.state\",\"payload\":{\"loggedIn\":true,\"userId\":\"@alice:example.org\"}}"));

        assertTrue(next.loggedIn);
        assertEquals("@alice:example.org", next.userId);
    }

    @Test
    public void recordsRoomListAndTimelineSnapshots() {
        RuntimeState state = new RuntimeState();

        RuntimeState withRooms = state.reduce(BridgeEvent.fromJson("{\"type\":\"rooms.snapshot\",\"payload\":{\"rooms\":[{\"roomId\":\"!a:hs\",\"name\":\"Hermes\",\"lastMessage\":\"hello\",\"unreadCount\":2,\"encrypted\":true}]}}"));
        RuntimeState withTimeline = withRooms.reduce(BridgeEvent.fromJson("{\"type\":\"timeline.snapshot\",\"payload\":{\"roomId\":\"!a:hs\",\"events\":[{\"eventId\":\"$1\",\"sender\":\"@alice:hs\",\"body\":\"| a | b |\\n| - | - |\",\"timestamp\":123,\"outgoing\":false}]}}"));

        assertEquals(1, withTimeline.rooms.size());
        assertEquals("Hermes", withTimeline.rooms.get(0).name);
        assertTrue(withTimeline.rooms.get(0).encrypted);
        assertEquals("!a:hs", withTimeline.selectedRoomId);
        assertEquals(1, withTimeline.timelineForSelectedRoom().size());
        assertEquals("| a | b |\n| - | - |", withTimeline.timelineForSelectedRoom().get(0).body);
    }

    @Test
    public void appendsTimelineEventsWithoutDroppingExistingEvents() {
        RuntimeState state = new RuntimeState()
                .reduce(BridgeEvent.fromJson("{\"type\":\"timeline.snapshot\",\"payload\":{\"roomId\":\"!a:hs\",\"events\":[{\"eventId\":\"$1\",\"sender\":\"@alice:hs\",\"body\":\"first\"}]}}"))
                .reduce(BridgeEvent.fromJson("{\"type\":\"timeline.append\",\"payload\":{\"roomId\":\"!a:hs\",\"event\":{\"eventId\":\"$2\",\"sender\":\"@bob:hs\",\"body\":\"second\",\"outgoing\":true}}}"));

        List<RuntimeState.TimelineEvent> events = state.timelineForRoom("!a:hs");

        assertEquals(2, events.size());
        assertEquals("first", events.get(0).body);
        assertEquals("second", events.get(1).body);
        assertTrue(events.get(1).outgoing);
    }

    @Test
    public void tracksTypingReactionsVerificationAndPushState() {
        RuntimeState state = new RuntimeState()
                .reduce(BridgeEvent.fromJson("{\"type\":\"timeline.snapshot\",\"payload\":{\"roomId\":\"!a:hs\",\"events\":[{\"eventId\":\"$1\",\"sender\":\"@alice:hs\",\"body\":\"hi\"}]}}"))
                .reduce(BridgeEvent.fromJson("{\"type\":\"typing.update\",\"payload\":{\"roomId\":\"!a:hs\",\"users\":[\"@bob:hs\"]}}"))
                .reduce(BridgeEvent.fromJson("{\"type\":\"reactions.update\",\"payload\":{\"eventId\":\"$1\",\"reactions\":[{\"key\":\"\\uD83D\\uDC4D\",\"count\":3,\"selected\":true}]}}"))
                .reduce(BridgeEvent.fromJson("{\"type\":\"verification.update\",\"payload\":{\"transactionId\":\"v1\",\"userId\":\"@bob:hs\",\"state\":\"requested\"}}"))
                .reduce(BridgeEvent.fromJson("{\"type\":\"push.registrationState\",\"payload\":{\"registered\":false,\"status\":\"distributor missing\"}}"));

        assertEquals("@bob:hs", state.typingUsersForRoom("!a:hs").get(0));
        assertEquals("\uD83D\uDC4D", state.reactionsForEvent("$1").get(0).key);
        assertEquals(3, state.reactionsForEvent("$1").get(0).count);
        assertTrue(state.reactionsForEvent("$1").get(0).selected);
        assertEquals("requested", state.verifications.get(0).state);
        assertFalse(state.pushRegistered);
        assertEquals("distributor missing", state.pushStatus);
    }

    @Test
    public void tracksCryptoRecoveryStateWithoutStoringRecoverySecret() {
        RuntimeState state = new RuntimeState()
                .reduce(BridgeEvent.fromJson("{\"type\":\"crypto.state\",\"payload\":{\"state\":\"recovery_required\",\"status\":\"Recovery key needed\",\"secretName\":\"m.megolm_backup.v1\"}}"));

        assertEquals("recovery_required", state.cryptoState);
        assertEquals("Recovery key needed", state.cryptoStatus);
        assertEquals("m.megolm_backup.v1", state.cryptoSecretName);
        assertTrue(state.cryptoRecoveryRequired);
    }
}
