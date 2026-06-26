package com.example.solatnearby;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PrayerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String prayerName = intent.getStringExtra("prayer_name");
        if (prayerName != null) {
            NotificationHelper.showPrayerNotification(context, prayerName);
        }
    }
}