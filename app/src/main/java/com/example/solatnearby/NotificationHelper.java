package com.example.solatnearby;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public class NotificationHelper {

    private static final String CHANNEL_ID = "solatnearby_alerts";

    // ── Arrival at masjid notification ───────────────────────
    public static void showArrivalNotification(Context context, String masjidName) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        createChannel(manager);

        Notification.Builder builder = getBuilder(context);
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("You are near the masjid")
                .setContentText("You have almost arrived at " + masjidName)
                .setAutoCancel(true);

        manager.notify(1001, builder.build());
    }

    // ── On prayer time notification ───────────────────────────
    // Shows when it is exactly prayer time
    // e.g. "Waktu Solat Asr — It's time for Asr prayer. Don't forget to pray!"
    public static void showPrayerNotification(Context context, String prayerName) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        createChannel(manager);

        Notification.Builder builder = getBuilder(context);
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("🕌 Waktu Solat " + prayerName)
                .setContentText("It's time for " + prayerName + " prayer. Don't forget to pray!")
                .setPriority(Notification.PRIORITY_HIGH)
                .setAutoCancel(true);

        manager.notify(prayerName.hashCode(), builder.build());
    }

    // ── Before prayer reminder notification ───────────────────
    // Shows X minutes before prayer time
    // e.g. "🕌 Prayer Reminder — Asr in 10 minutes. Prepare for prayer!"
    public static void showReminderNotification(Context context, String prayerName, int minutesBefore) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        createChannel(manager);

        Notification.Builder builder = getBuilder(context);
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("🕌 Prayer Reminder")
                .setContentText(prayerName + " in " + minutesBefore + " minutes. Prepare for prayer!")
                .setPriority(Notification.PRIORITY_HIGH)
                .setAutoCancel(true);

        // Use different notification ID from on-time so both can show
        manager.notify((prayerName + "_reminder").hashCode(), builder.build());
    }

    // ── Helpers ───────────────────────────────────────────────
    private static void createChannel(NotificationManager manager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "SolatNearby Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Arrival and prayer time alerts");
            manager.createNotificationChannel(channel);
        }
    }

    private static Notification.Builder getBuilder(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Notification.Builder(context, CHANNEL_ID);
        } else {
            return new Notification.Builder(context);
        }
    }
}