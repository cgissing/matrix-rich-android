package io.github.cgissing.matrixrich;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MatrixVerificationStateTest {
    @Test
    public void mapsVerificationPayloadForNativeControls() throws Exception {
        JSONObject payload = new JSONObject()
                .put("transactionId", "txn1")
                .put("otherUserId", "@alice:example.org")
                .put("otherDeviceId", "ALICEDEVICE")
                .put("isSelfVerification", true)
                .put("phaseCode", 4)
                .put("phase", "Started")
                .put("source", "incoming")
                .put("canStartSas", false)
                .put("canConfirmSas", true)
                .put("canShowQr", true)
                .put("canScanQr", true)
                .put("canConfirmQr", true)
                .put("sasDecimal", "123 456 789")
                .put("sasEmoji", "A Alpha\nB Bravo")
                .put("qrCodeBase64", "TUFUUklY");

        MatrixVerificationState state = MatrixRuntimeMapper.verificationFromPayload(payload);

        assertEquals("txn1", state.transactionId);
        assertEquals("@alice:example.org", state.otherUserId);
        assertEquals("ALICEDEVICE", state.otherDeviceId);
        assertTrue(state.selfVerification);
        assertEquals("Started", state.phase);
        assertEquals("incoming", state.source);
        assertTrue(state.canConfirmSas);
        assertTrue(state.canShowQr);
        assertTrue(state.canScanQr);
        assertTrue(state.canConfirmQr);
        assertEquals("123 456 789", state.sasDecimal);
        assertEquals("A Alpha\nB Bravo", state.sasEmoji);
        assertEquals("TUFUUklY", state.qrCodeBase64);
    }

    @Test
    public void buildsReadableSummary() throws Exception {
        JSONObject payload = new JSONObject()
                .put("otherUserId", "@alice:example.org")
                .put("otherDeviceId", "ALICEDEVICE")
                .put("phase", "Ready")
                .put("source", "outgoing");

        MatrixVerificationState state = MatrixRuntimeMapper.verificationFromPayload(payload);

        assertEquals("Verification Ready with @alice:example.org / ALICEDEVICE", state.summary());
    }
}
