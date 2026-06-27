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
            // Strip timezone: "05:43 (MYT)" → "05:43"
            String cleanTime = timeStr.split(" ")[0];
            String[] parts = cleanTime.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            // Skip if already passed today
            if (cal.before(Calendar.getInstance())) {
                Log.d("PrayerScheduler", prayerName + " already passed, skipping.");
                return;
            }

            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;

            // Android 12+ — check exact alarm permission first
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!am.canScheduleExactAlarms()) {
                    Log.e("PrayerScheduler", "Exact alarm permission not granted!");
                    return;
                }
            }

            Intent intent = new Intent(context, PrayerReceiver.class);
            intent.putExtra("prayer_name", prayerName);

            PendingIntent pi = PendingIntent.getBroadcast(
                    context, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            }

            Log.d("PrayerScheduler", "Scheduled " + prayerName + " at " + cleanTime);

        } catch (Exception e) {
            Log.e("PrayerScheduler", "Error scheduling " + prayerName + ": " + e.getMessage());
        }
    }
}