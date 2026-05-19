package io.github.cgissing.matrixrich;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MatrixLoginFormStateTest {
    @Test
    public void passwordLoginOnlyRequiresAccountAndPassword() {
        MatrixLoginFormState state = new MatrixLoginFormState(
                "@alice:example.org", "secret", "", "", ""
        );

        assertTrue(state.usesPasswordLogin());
        assertNull(state.validationError());
    }

    @Test
    public void passwordLoginDoesNotRequireDeviceOrAccessToken() {
        MatrixLoginFormState state = new MatrixLoginFormState(
                "alice", "secret", "", "", ""
        );

        assertNull(state.validationError());
    }

    @Test
    public void emptyPasswordDefaultsToPasswordLoginPromptWhenNoTokenIsProvided() {
        MatrixLoginFormState state = new MatrixLoginFormState(
                "alice", "", "", "", ""
        );

        assertFalse(state.usesPasswordLogin());
        assertEquals("Enter a password", state.validationError());
    }

    @Test
    public void tokenRestoreRequiresUserIdAndDeviceId() {
        MatrixLoginFormState state = new MatrixLoginFormState(
                "", "", "tok", "@alice:example.org", ""
        );

        assertEquals("Access-token restore needs user ID and device ID for E2EE", state.validationError());
    }
}
