package io.github.cgissing.matrixrich;

public final class MatrixApiException extends Exception {
    public MatrixApiException(String message) {
        super(message);
    }

    public MatrixApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
