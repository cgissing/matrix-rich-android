package io.github.cgissing.matrixrich;

import android.content.Context;
import android.content.SharedPreferences;

final class AppPrefs {
    static final String PREFS = "matrix_rich";
    static final String KEY_HOMESERVER_URL = "homeserver_url";
    static final String KEY_ACCOUNT_HINT = "account_hint";
    static final String KEY_PUSH_ENABLED = "push_enabled";
    static final String KEY_NTFY_SERVER = "ntfy_server";
    static final String KEY_NTFY_TOPIC = "ntfy_topic";
    static final String KEY_NTFY_TOKEN = "ntfy_token";

    static final String DEFAULT_HOMESERVER_URL = "https://matrix.org";
    static final String DEFAULT_NTFY_SERVER = "https://ntfy.sh";

    private AppPrefs() {
    }

    static SharedPreferences get(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static String homeserverUrl(Context context) {
        return get(context).getString(KEY_HOMESERVER_URL, DEFAULT_HOMESERVER_URL);
    }

    static String accountHint(Context context) {
        return get(context).getString(KEY_ACCOUNT_HINT, "");
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
