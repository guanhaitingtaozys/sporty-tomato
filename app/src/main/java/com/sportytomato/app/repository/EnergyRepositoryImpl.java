package com.sportytomato.app.repository;

import com.sportytomato.app.model.database.AppDatabase;
import com.sportytomato.app.model.entity.EnergyRecord;
import com.sportytomato.app.model.entity.StepRecord;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EnergyRepositoryImpl implements EnergyRepository {
    private static final int DAILY_ENERGY_CAP = 2000;
    private static final int MAX_OVERFLOW_CAP = 500;
    private final AppDatabase database;
    private final ExecutorService executor;
    private static final Object stepsLock = new Object();

    public EnergyRepositoryImpl(AppDatabase database) { this.database = database; this.executor = Executors.newSingleThreadExecutor(); }

    @Override public int getTodayEnergy(int userId) { String today = getTodayDate(); return database.energyRecordDao().getTodayNetEnergy(userId, today); }
    @Override public int getTodayStepEnergy(int userId) { String today = getTodayDate(); return database.energyRecordDao().getTodayStepEnergy(userId, today); }
    @Override public int getTodaySteps(int userId) { String today = getTodayDate(); return database.stepRecordDao().getTodayTotalSteps(userId, today); }
    @Override public void addSteps(int userId, int steps) { executor.execute(() -> addStepsInternal(userId, steps)); }
    @Override public void addStepsSync(int userId, int steps) { addStepsInternal(userId, steps); }

    private void addStepsInternal(int userId, int steps) {
        synchronized (stepsLock) {
            long now = System.currentTimeMillis(); String today = getTodayDate();
            StepRecord stepRecord = new StepRecord(); stepRecord.setUserId(userId); stepRecord.setStepCount(steps); stepRecord.setRecordDate(today); stepRecord.setRecordTime(now); stepRecord.setSourceType(1); stepRecord.setSynced(0);
            database.stepRecordDao().insertStepRecord(stepRecord);
            int currentEnergy = database.energyRecordDao().getTodayNetEnergy(userId, today);
            int energyGain = steps / 10; int isCapped = 0;
            if (currentEnergy >= DAILY_ENERGY_CAP) { addOverflowEnergy(userId, energyGain); return; }
            int newEnergy = currentEnergy + energyGain;
            if (newEnergy > DAILY_ENERGY_CAP) { int cappedGain = DAILY_ENERGY_CAP - currentEnergy; int overflow = energyGain - cappedGain; energyGain = cappedGain; isCapped = 1; addOverflowEnergy(userId, overflow); }
            if (energyGain > 0) {
                EnergyRecord er = new EnergyRecord(); er.setUserId(userId); er.setEnergyValue(energyGain); er.setRecordDate(today); er.setSourceType(0); er.setCapped(isCapped); er.setCreatedAt(now);
                database.energyRecordDao().insertEnergyRecord(er);
            }
        }
    }

    private void addOverflowEnergy(int userId, int overflow) {
        int overflow50 = overflow / 2; if (overflow50 <= 0) return;
        String tomorrow = getTomorrowDate(); int existingOverflow = getEnergyBySourceType(userId, tomorrow, 2);
        int remaining = MAX_OVERFLOW_CAP - existingOverflow; if (remaining <= 0) return;
        int toStore = Math.min(overflow50, remaining);
        EnergyRecord record = new EnergyRecord(); record.setUserId(userId); record.setEnergyValue(toStore); record.setRecordDate(tomorrow); record.setSourceType(2); record.setCapped(0); record.setCreatedAt(System.currentTimeMillis());
        database.energyRecordDao().insertEnergyRecord(record);
    }

    @Override public void consumeEnergy(int userId, int amount) { consumeEnergyWithType(userId, amount, 1); }
    @Override public void consumeEnergyWithType(int userId, int amount, int sourceType) {
        if (amount <= 0) return;
        executor.execute(() -> {
            String today = getTodayDate(); int currentEnergy = database.energyRecordDao().getTodayNetEnergy(userId, today);
            if (currentEnergy < amount) return;
            EnergyRecord er = new EnergyRecord(); er.setUserId(userId); er.setEnergyValue(amount); er.setRecordDate(today); er.setSourceType(sourceType); er.setCapped(0); er.setCreatedAt(System.currentTimeMillis());
            database.energyRecordDao().insertEnergyRecord(er);
        });
    }
    @Override public int getDailyNetEnergy(int userId, String date) { return database.energyRecordDao().getTodayNetEnergy(userId, date); }
    @Override public int getEnergyBySourceType(int userId, String date, int sourceType) { Integer r = database.energyRecordDao().getEnergyBySourceType(userId, date, sourceType); return r != null ? r : 0; }
    @Override public String getTodayDate() { return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()); }
    private String getTomorrowDate() { Calendar cal = Calendar.getInstance(); cal.add(Calendar.DAY_OF_MONTH, 1); return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime()); }
    public void shutdown() { executor.shutdown(); }
}