package io.github.cgissing.matrixrich;

public final class MatrixLoginFormState {
    private final String account;
    private final String password;
    private final String accessToken;
    private final String userId;
    private final String deviceId;

    public MatrixLoginFormState(String account, String password, String accessToken, String userId, String deviceId) {
        this.account = clean(account);
        this.password = password == null ? "" : password;
        this.accessToken = clean(accessToken);
        this.userId = clean(userId);
        this.deviceId = clean(deviceId);
    }

    public boolean usesPasswordLogin() {
        return !password.isEmpty();
    }

    public String validationError() {
        if (usesPasswordLogin()) {
            return account.isEmpty() ? "Enter a login name" : null;
        }
        if (accessToken.isEmpty()) {
            return "Enter a password";
        }
        if (userId.isEmpty() || deviceId.isEmpty()) {
            return "Access-token restore needs user ID and device ID for E2EE";
        }
        return null;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
