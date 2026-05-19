package chat.richclient.push;

import android.content.Context;
import android.content.Intent;
import org.unifiedpush.android.connector.MessagingReceiver;

public final class RichUnifiedPushReceiver extends MessagingReceiver {
    public static final String ACTION_NEW_ENDPOINT = "chat.richclient.push.NEW_ENDPOINT";
    public static final String ACTION_MESSAGE = "chat.richclient.push.MESSAGE";
    public static final String ACTION_REGISTRATION_FAILED = "chat.richclient.push.REGISTRATION_FAILED";
    public static final String ACTION_UNREGISTERED = "chat.richclient.push.UNREGISTERED";
    public static final String EXTRA_ENDPOINT = "endpoint";
    public static final String EXTRA_MESSAGE = "message";

    private static final String PREFS = "rich_unifiedpush";
    private static final String KEY_ENDPOINT = "endpoint";

    public static String storedEndpoint(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_ENDPOINT, "");
    }

    @Override
    public void onMessage(Context context, byte[] message, String instance) {
        Intent intent = new Intent(ACTION_MESSAGE);
        intent.putExtra(EXTRA_MESSAGE, message);
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent);
    }

    @Override
    public void onNewEndpoint(Context context, String endpoint, String instance) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ENDPOINT, endpoint)
                .apply();
        Intent intent = new Intent(ACTION_NEW_ENDPOINT);
        intent.putExtra(EXTRA_ENDPOINT, endpoint);
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent);
    }

    @Override
    public void onRegistrationFailed(Context context, String instance) {
        Intent intent = new Intent(ACTION_REGISTRATION_FAILED);
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent);
    }

    @Override
    public void onUnregistered(Context context, String instance) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_ENDPOINT)
                .apply();
        Intent intent = new Intent(ACTION_UNREGISTERED);
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent);
    }
}
