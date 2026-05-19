package io.github.cgissing.matrixrich;

import android.content.Context;

import java.net.URI;

public final class ElementWebConfig {
    private static final String MOBILE_REDIRECT_COOKIE_NAME = "element_mobile_redirect_to_guide";

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

    static String mobileRedirectBypassCookie() {
        return MOBILE_REDIRECT_COOKIE_NAME + "=false; path=/; max-age=31536000";
    }

    static boolean isMobileGuideOrAppHandoffUrl(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        try {
            URI uri = URI.create(trimmed);
            String host = uri.getHost();
            if ("mobile.element.io".equalsIgnoreCase(host)) {
                return true;
            }
            String path = uri.getPath();
            return path != null && (path.endsWith("/mobile_guide") || path.contains("/mobile_guide/"));
        } catch (Exception ignored) {
            return false;
        }
    }
}
