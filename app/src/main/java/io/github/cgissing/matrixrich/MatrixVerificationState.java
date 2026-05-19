package io.github.cgissing.matrixrich;

public final class MatrixVerificationState {
    public final String transactionId;
    public final String otherUserId;
    public final String otherDeviceId;
    public final boolean selfVerification;
    public final int phaseCode;
    public final String phase;
    public final String source;
    public final boolean canAccept;
    public final boolean canStartSas;
    public final boolean canConfirmSas;
    public final boolean canShowQr;
    public final boolean canScanQr;
    public final boolean canConfirmQr;
    public final String sasDecimal;
    public final String sasEmoji;
    public final String qrCodeBase64;

    public MatrixVerificationState(
            String transactionId,
            String otherUserId,
            String otherDeviceId,
            boolean selfVerification,
            int phaseCode,
            String phase,
            String source,
            boolean canAccept,
            boolean canStartSas,
            boolean canConfirmSas,
            boolean canShowQr,
            boolean canScanQr,
            boolean canConfirmQr,
            String sasDecimal,
            String sasEmoji,
            String qrCodeBase64
    ) {
        this.transactionId = clean(transactionId);
        this.otherUserId = clean(otherUserId);
        this.otherDeviceId = clean(otherDeviceId);
        this.selfVerification = selfVerification;
        this.phaseCode = phaseCode;
        this.phase = clean(phase).isEmpty() ? "None" : clean(phase);
        this.source = clean(source);
        this.canAccept = canAccept;
        this.canStartSas = canStartSas;
        this.canConfirmSas = canConfirmSas;
        this.canShowQr = canShowQr;
        this.canScanQr = canScanQr;
        this.canConfirmQr = canConfirmQr;
        this.sasDecimal = clean(sasDecimal);
        this.sasEmoji = clean(sasEmoji);
        this.qrCodeBase64 = clean(qrCodeBase64);
    }

    public static MatrixVerificationState empty() {
        return new MatrixVerificationState("", "", "", false, 0, "None", "", false, false, false, false, false, false, "", "", "");
    }

    public String summary() {
        if ("None".equals(phase)) {
            return "No active verification";
        }
        String target = otherUserId.isEmpty() ? "another device" : otherUserId;
        if (!otherDeviceId.isEmpty()) {
            target += " / " + otherDeviceId;
        }
        return "Verification " + phase + " with " + target;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
