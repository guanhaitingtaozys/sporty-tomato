package com.sportytomato.app.util;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import java.util.Calendar;

public class NotificationHelper {
    public static final String CHANNEL_ENERGY = "energy_reminder";
    public static final String CHANNEL_TOMATO = "tomato_notify";
    public static final String CHANNEL_DRY = "dry_warning";
    private static final String ACTION_ENERGY_REMINDER = "com.sportytomato.app.ENERGY_REMINDER";
    private static final int ENERGY_REMINDER_REQUEST_CODE = 1001;

    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(new NotificationChannel(CHANNEL_ENERGY, "能量提醒", NotificationManager.IMPORTANCE_DEFAULT));
            manager.createNotificationChannel(new NotificationChannel(CHANNEL_TOMATO, "番茄通知", NotificationManager.IMPORTANCE_HIGH));
            manager.createNotificationChannel(new NotificationChannel(CHANNEL_DRY, "干涸预警", NotificationManager.IMPORTANCE_HIGH));
        }
    }

    public static void scheduleEnergyReminder(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, EnergyReminderReceiver.class);
        intent.setAction(ACTION_ENERGY_REMINDER);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, ENERGY_REMINDER_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Calendar calendar = Calendar.getInstance(); calendar.set(Calendar.HOUR_OF_DAY, 20); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0);
        if (System.currentTimeMillis() > calendar.getTimeInMillis()) calendar.add(Calendar.DAY_OF_YEAR, 1);
        try { alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent); } catch (SecurityException e) { alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent); }
    }

    public static void cancelEnergyReminder(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, EnergyReminderReceiver.class); intent.setAction(ACTION_ENERGY_REMINDER);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, ENERGY_REMINDER_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pendingIntent);
    }

    public static void sendTomatoRipeNotification(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("notification_enabled", true)) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.notify(2001, new NotificationCompat.Builder(context, CHANNEL_TOMATO).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("番茄成熟啦！").setContentText("你的番茄已经成熟，快去收获吧~").setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).build());
    }

    public static void sendLowEnergyNotification(Context context, int currentEnergy) {
        SharedPreferences prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("notification_enabled", true)) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.notify(2002, new NotificationCompat.Builder(context, CHANNEL_ENERGY).setSmallIcon(android.R.drawable.ic_dialog_alert).setContentTitle("能量不足提醒").setContentText("当前能量仅剩 " + currentEnergy + " 点，快通过步数补充能量吧！").setPriority(NotificationCompat.PRIORITY_DEFAULT).setAutoCancel(true).build());
    }

    public static void sendDryWarningNotification(Context context, String remainingTime) {
        SharedPreferences prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("notification_enabled", true)) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.notify(2003, new NotificationCompat.Builder(context, CHANNEL_DRY).setSmallIcon(android.R.drawable.ic_dialog_alert).setContentTitle("番茄即将干涸！").setContentText("距离干涸惩罚还剩 " + remainingTime + "，快去浇水吧~").setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).build());
    }

    public static class EnergyReminderReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.notify(3001, new NotificationCompat.Builder(context, CHANNEL_ENERGY).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("能量提醒").setContentText("今日能量即将重置，记得使用剩余能量哦！").setPriority(NotificationCompat.PRIORITY_DEFAULT).setAutoCancel(true).build());
        }
    }
}