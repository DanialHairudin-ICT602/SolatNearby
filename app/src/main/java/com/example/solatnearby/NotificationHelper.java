package com.example.solatnearby;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public class NotificationHelper {

    private static final String CHANNEL_ID = "solatnearby_alerts";

    public static void showArrivalNotification(Context context, String masjidName) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "SolatNearby Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Arrival and prayer time alerts");
            manager.createNotificationChannel(channel);
        }

        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context);
        }

        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("You are near the masjid")
                .setContentText("You have almost arrived at " + masjidName)
                .setAutoCancel(true);

        manager.notify(1001, builder.build());
    }
}