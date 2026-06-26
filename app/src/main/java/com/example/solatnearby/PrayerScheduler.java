package com.example.solatnearby;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;

public class PrayerScheduler {
    private final Context context;

    public PrayerScheduler(Context context) {
        this.context = context;
    }

    public void schedule(String prayerName, String timeStr, int requestCode) {
        try {
            // timeStr format from Aladhan: "05:43 (MYT)" — strip the timezone part
            String cleanTime = timeStr.split(" ")[0];
            String[] parts = cleanTime.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            cal.set(Calendar.SECOND, 0);

            // If the prayer time has already passed today, skip
            if (cal.before(Calendar.getInstance())) return;

            Intent intent = new Intent(context, PrayerReceiver.class);
            intent.putExtra("prayer_name", prayerName);

            PendingIntent pi = PendingIntent.getBroadcast(
                    context, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            }

        } catch (Exception e) {
            Log.e("PrayerScheduler", "Error scheduling " + prayerName + ": " + e.getMessage());
        }
    }
}