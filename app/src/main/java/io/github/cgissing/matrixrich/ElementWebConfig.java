package io.github.cgissing.matrixrich;

import android.content.Context;

public final class ElementWebConfig {
    private ElementWebConfig() {
    }

    public static String initialUrl(Context context) {
        return normalizeElementWebUrl(AppPrefs.elementWebUrl(context));
    }

    static String normalizeElementWebUrl(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            trimmed = AppPrefs.DEFAULT_ELEMENT_WEB_URL;
        }
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "https://" + trimmed;
        }
        return trimmed.endsWith("/") ? trimmed : trimmed + "/";
    }
}
