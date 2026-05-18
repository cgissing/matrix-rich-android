package io.github.cgissing.matrixrich;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class NtfyPushService extends Service {
    private static final int STATUS_NOTIFICATION_ID = 10;
    private static final String CHANNEL_STATUS = "ntfy_status";
    private static final String CHANNEL_MESSAGES = "matrix_messages";

    private volatile boolean running;
    private Thread worker;
    private String lastDisplayedId;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannels();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String topic = AppPrefs.ntfyTopic(this).trim();
        if (!AppPrefs.pushEnabled(this) || topic.isEmpty()) {
            stopSelf();
            return START_NOT_STICKY;
        }
        startForeground(STATUS_NOTIFICATION_ID, statusNotification());
        startWorker();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
        if (worker != null) {
            worker.interrupt();
            worker = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startWorker() {
        if (worker != null && worker.isAlive()) {
            return;
        }
        running = true;
        worker = new Thread(this::runLoop, "ntfy-push-listener");
        worker.start();
    }

    private void runLoop() {
        long delayMs = 2000L;
        while (running) {
            try {
                streamOnce();
                delayMs = 2000L;
            } catch (Exception ignored) {
                sleep(delayMs);
                delayMs = Math.min(delayMs * 2L, 60000L);
            }
        }
    }

    private void streamOnce() throws Exception {
        String endpoint = NtfyEndpoint.jsonStreamUrl(AppPrefs.ntfyServer(this), AppPrefs.ntfyTopic(this));
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(0);
        connection.setRequestProperty("Accept", "application/x-ndjson");
        String token = AppPrefs.ntfyToken(this).trim();
        if (!token.isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + token);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while (running && (line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                NtfyMessage message = NtfyMessage.fromJsonLine(line);
                if (!message.isDisplayable()) {
                    continue;
                }
                if (message.id != null && message.id.equals(lastDisplayedId)) {
                    continue;
                }
                lastDisplayedId = message.id;
                showMessageNotification(message);
            }
        } finally {
            connection.disconnect();
        }
    }

    private void showMessageNotification(NtfyMessage message) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent open = new Intent(this, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (message.click != null && !message.click.isEmpty()) {
            if (NtfyMessage.isClientDeepLink(message.click)) {
                open.setAction(Intent.ACTION_VIEW);
                open.setData(Uri.parse(message.click));
            } else {
                open.putExtra("open_url", message.click);
            }
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                message.id == null ? 0 : message.id.hashCode(),
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | immutableFlag()
        );

        Notification.Builder builder = notificationBuilder(CHANNEL_MESSAGES)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle(message.notificationTitle(getString(getApplicationInfo().labelRes)))
                .setContentText(message.notificationBody())
                .setStyle(new Notification.BigTextStyle().bigText(message.notificationBody()))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        manager.notify(message.id == null ? (int) System.currentTimeMillis() : message.id.hashCode(), builder.build());
    }

    private Notification statusNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, open, PendingIntent.FLAG_UPDATE_CURRENT | immutableFlag());
        return notificationBuilder(CHANNEL_STATUS)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Matrix Rich push is active")
                .setContentText(AppPrefs.ntfyServer(this) + "/" + AppPrefs.ntfyTopic(this))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private Notification.Builder notificationBuilder(String channelId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Notification.Builder(this, channelId);
        }
        return new Notification.Builder(this);
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel status = new NotificationChannel(CHANNEL_STATUS, "ntfy listener", NotificationManager.IMPORTANCE_LOW);
        NotificationChannel messages = new NotificationChannel(CHANNEL_MESSAGES, "Matrix messages", NotificationManager.IMPORTANCE_HIGH);
        manager.createNotificationChannel(status);
        manager.createNotificationChannel(messages);
    }

    private int immutableFlag() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
    }

    private void sleep(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
