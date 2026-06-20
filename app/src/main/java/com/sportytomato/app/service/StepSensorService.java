package com.sportytomato.app.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.sportytomato.app.model.entity.User;
import com.sportytomato.app.view.SportyTomatoApplication;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StepSensorService implements SensorEventListener {
    private static final String PREFS_NAME = "step_sensor_prefs";
    private static final String KEY_ACCUMULATED = "accumulated_offset";
    private static final String KEY_LAST_RAW = "last_raw_value";
    private static final String KEY_TODAY_BASELINE = "today_baseline";
    private static final String KEY_TODAY_DATE = "today_date";
    private static volatile StepSensorService instance;
    private final SensorManager sensorManager;
    private final Sensor stepSensor;
    private final SharedPreferences prefs;
    private final Context appContext;
    private volatile long accumulatedOffset, lastRawValue, todayBaseline, healthConnectSteps;
    private volatile String todayDate;

    private StepSensorService(Context context) {
        appContext = context.getApplicationContext();
        sensorManager = (SensorManager) appContext.getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized StepSensorService getInstance(Context context) {
        if (instance == null) instance = new StepSensorService(context);
        return instance;
    }

    public void startListening() {
        accumulatedOffset = prefs.getLong(KEY_ACCUMULATED, 0L);
        lastRawValue = prefs.getLong(KEY_LAST_RAW, 0L);
        todayBaseline = prefs.getLong(KEY_TODAY_BASELINE, 0L);
        todayDate = prefs.getString(KEY_TODAY_DATE, "");
        try { HealthConnectService hc = HealthConnectService.getInstance(appContext); if (hc.isAvailable()) hc.readTodaySteps(steps -> healthConnectSteps = steps); } catch (Exception e) {}
        if (stepSensor != null) sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void stopListening() { sensorManager.unregisterListener(this); }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long rawValue = (long) event.values[0];
        if (rawValue < lastRawValue) { accumulatedOffset += lastRawValue; prefs.edit().putLong(KEY_ACCUMULATED, accumulatedOffset).commit(); }
        lastRawValue = rawValue; prefs.edit().putLong(KEY_LAST_RAW, lastRawValue).apply();
        long totalSteps = accumulatedOffset + rawValue;
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        if (!currentDate.equals(todayDate)) {
            todayBaseline = prefs.getLong("_prev_total", totalSteps); todayDate = currentDate;
            prefs.edit().putLong(KEY_TODAY_BASELINE, todayBaseline).putString(KEY_TODAY_DATE, todayDate).putLong("last_synced_steps", 0L).apply();
        }
        if (todayBaseline == 0) { todayBaseline = totalSteps; todayDate = currentDate; prefs.edit().putLong(KEY_TODAY_BASELINE, todayBaseline).putString(KEY_TODAY_DATE, todayDate).apply(); }
        long todaySensorSteps = Math.max(0, totalSteps - todayBaseline);
        prefs.edit().putLong("_prev_total", totalSteps).apply();

        long lastSyncedSteps = prefs.getLong("last_synced_steps", 0L);
        if (todaySensorSteps >= lastSyncedSteps + 50) {
            User currentUser = RepositoryManager.getInstance().getUserRepo().getCurrentUser();
            if (currentUser != null) {
                int stepsToAdd = (int) (todaySensorSteps - lastSyncedSteps);
                RepositoryManager.getInstance().getEnergyRepo().addStepsSync(currentUser.getUserId(), stepsToAdd);
                prefs.edit().putLong("last_synced_steps", todaySensorSteps).apply();
            }
        }
        if (todaySensorSteps >= 10000 && RepositoryManager.getInstance().getUserRepo().getCurrentUser() != null) {
            try { ((SportyTomatoApplication) appContext).getAchievementService().checkAndUnlock(RepositoryManager.getInstance().getUserRepo().getCurrentUser().getUserId(), 4, (int) todaySensorSteps); } catch (Exception ignored) {}
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public long getTodaySensorSteps() { return Math.max(Math.max(0, accumulatedOffset + lastRawValue - todayBaseline), healthConnectSteps); }
    public long getTodayHardwareSteps() { return Math.max(0, accumulatedOffset + lastRawValue - todayBaseline); }
    public long getHealthConnectSteps() { return healthConnectSteps; }

    public void syncPendingSteps() {
        User currentUser = RepositoryManager.getInstance().getUserRepo().getCurrentUser();
        if (currentUser == null) return;
        long sensorSteps = getTodayHardwareSteps();
        int dbSteps = RepositoryManager.getInstance().getEnergyRepo().getTodaySteps(currentUser.getUserId());
        int pending = (int) (sensorSteps - dbSteps);
        if (pending > 0) { RepositoryManager.getInstance().getEnergyRepo().addStepsSync(currentUser.getUserId(), pending); prefs.edit().putLong("last_synced_steps", sensorSteps).apply(); }
    }

    public void refreshHealthConnectSteps() {
        try { HealthConnectService hc = HealthConnectService.getInstance(appContext); if (hc.isAvailable()) hc.readTodaySteps(steps -> healthConnectSteps = steps); } catch (Exception e) {}
    }
}