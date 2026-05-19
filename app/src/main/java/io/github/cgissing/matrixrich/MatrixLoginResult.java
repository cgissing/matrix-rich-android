package io.github.cgissing.matrixrich;

public final class MatrixLoginResult {
    public final String accessToken;
    public final String userId;
    public final String deviceId;

    public MatrixLoginResult(String accessToken, String userId, String deviceId) {
        this.accessToken = accessToken == null ? "" : accessToken;
        this.userId = userId == null ? "" : userId;
        this.deviceId = deviceId == null ? "" : deviceId;
    }
}
