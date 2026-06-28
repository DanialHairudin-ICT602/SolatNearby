package com.example.solatnearby;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class PrayerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("PrayerReceiver", "Alarm received!");

        String prayerName  = intent.getStringExtra("prayer_name");
        boolean isReminder = intent.getBooleanExtra("is_reminder", false);
        int minutesBefore  = intent.getIntExtra("minutes_before", 10);

        if (prayerName == null) return;

        if (isReminder) {
            // Before prayer — "Asr in 10 minutes"
            NotificationHelper.showReminderNotification(context, prayerName, minutesBefore);
        } else {
            // On prayer time — "Waktu Solat Asr"
            NotificationHelper.showPrayerNotification(context, prayerName);
        }
    }
}