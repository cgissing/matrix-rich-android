package io.github.cgissing.matrixrich;

public final class NativeReaction {
    public final String key;
    public final int count;
    public final boolean reactedByMe;

    public NativeReaction(String key, int count, boolean reactedByMe) {
        this.key = key == null ? "" : key;
        this.count = Math.max(0, count);
        this.reactedByMe = reactedByMe;
    }
}
