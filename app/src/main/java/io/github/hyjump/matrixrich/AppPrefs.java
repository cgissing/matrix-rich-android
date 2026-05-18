package io.github.hyjump.matrixrich;

import android.content.Context;
import android.content.SharedPreferences;

final class AppPrefs {
    static final String PREFS = "matrix_rich";
    static final String KEY_ELEMENT_URL = "element_url";
    static final String KEY_DESKTOP_USER_AGENT = "desktop_user_agent";
    static final String KEY_PUSH_ENABLED = "push_enabled";
    static final String KEY_NTFY_SERVER = "ntfy_server";
    static final String KEY_NTFY_TOPIC = "ntfy_topic";
    static final String KEY_NTFY_TOKEN = "ntfy_token";

    static final String DEFAULT_ELEMENT_URL = "https://app.element.io/";
    static final String DEFAULT_NTFY_SERVER = "https://ntfy.sh";

    private AppPrefs() {
    }

    static SharedPreferences get(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static String elementUrl(Context context) {
        return get(context).getString(KEY_ELEMENT_URL, DEFAULT_ELEMENT_URL);
    }

    static boolean desktopUserAgent(Context context) {
        return get(context).getBoolean(KEY_DESKTOP_USER_AGENT, true);
    }

    static boolean pushEnabled(Context context) {
        return get(context).getBoolean(KEY_PUSH_ENABLED, false);
    }

    static String ntfyServer(Context context) {
        return get(context).getString(KEY_NTFY_SERVER, DEFAULT_NTFY_SERVER);
    }

    static String ntfyTopic(Context context) {
        return get(context).getString(KEY_NTFY_TOPIC, "");
    }

    static String ntfyToken(Context context) {
        return get(context).getString(KEY_NTFY_TOKEN, "");
    }
}
