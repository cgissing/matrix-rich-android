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
    public final String sasDecimal;
    public final String sasEmoji;

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
            String sasDecimal,
            String sasEmoji
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
        this.sasDecimal = clean(sasDecimal);
        this.sasEmoji = clean(sasEmoji);
    }

    public static MatrixVerificationState empty() {
        return new MatrixVerificationState("", "", "", false, 0, "None", "", false, false, false, "", "");
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
