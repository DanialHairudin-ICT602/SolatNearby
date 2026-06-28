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
    private static final int REMINDER_MINUTES_BEFORE = 20;

    public PrayerScheduler(Context context) {
        this.context = context;
    }

    public void schedule(String prayerName, String timeStr, int requestCode) {
        try {
            // Strip timezone: "05:43 (MYT)" → "05:43"
            String cleanTime = timeStr.split(" ")[0];
            String[] parts = cleanTime.split(":");
            int hour   = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);

            // ── Prayer time calendar ─────────────────────────────────
            Calendar prayerCal = Calendar.getInstance();
            prayerCal.set(Calendar.HOUR_OF_DAY, hour);
            prayerCal.set(Calendar.MINUTE, minute);
            prayerCal.set(Calendar.SECOND, 0);
            prayerCal.set(Calendar.MILLISECOND, 0);

            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;

            // Android 12+ exact alarm permission check
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!am.canScheduleExactAlarms()) {
                    Log.e("PrayerScheduler", "Exact alarm permission not granted!");
                    return;
                }
            }

            Calendar now = Calendar.getInstance();

            // ── 1. Schedule ON prayer time notification ──────────────
            if (prayerCal.after(now)) {
                scheduleAlarm(am, prayerName, prayerCal.getTimeInMillis(),
                        requestCode * 10, false);
                Log.d("PrayerScheduler", "Scheduled ON-TIME for " + prayerName + " at " + cleanTime);
            } else {
                Log.d("PrayerScheduler", prayerName + " already passed, skipping on-time.");
            }

            // ── 2. Schedule BEFORE prayer reminder ───────────────────
            Calendar reminderCal = (Calendar) prayerCal.clone();
            reminderCal.add(Calendar.MINUTE, -REMINDER_MINUTES_BEFORE);

            if (reminderCal.after(now)) {
                scheduleAlarm(am, prayerName, reminderCal.getTimeInMillis(),
                        requestCode * 10 + 1, true);
                Log.d("PrayerScheduler", "Scheduled REMINDER for " + prayerName
                        + " (" + REMINDER_MINUTES_BEFORE + " mins before)");
            } else {
                Log.d("PrayerScheduler", prayerName + " reminder already passed, skipping.");
            }

        } catch (Exception e) {
            Log.e("PrayerScheduler", "Error scheduling " + prayerName + ": " + e.getMessage());
        }
    }

    private void scheduleAlarm(AlarmManager am, String prayerName,
                               long triggerMillis, int requestCode, boolean isReminder) {
        Intent intent = new Intent(context, PrayerReceiver.class);
        intent.putExtra("prayer_name", prayerName);
        intent.putExtra("is_reminder", isReminder);
        intent.putExtra("minutes_before", REMINDER_MINUTES_BEFORE);

        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pi);
        }
    }
}