package com.sportytomato.app.repository;

import androidx.lifecycle.LiveData;
import com.sportytomato.app.model.database.AppDatabase;
import com.sportytomato.app.model.entity.*;
import com.sportytomato.app.service.EMASClient;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class TomatoRepositoryImpl implements TomatoRepository {
    private static final int WATER_ENERGY_COST = 100;
    private static final int WATER_PROGRESS_GAIN = 20;
    private final AppDatabase database;
    private static final Object waterLock = new Object();

    public TomatoRepositoryImpl(AppDatabase database) { this.database = database; }

    @Override public LiveData<TomatoState> getTomatoLive(int userId) { return database.tomatoDao().getTomatoByUserIdLive(userId); }
    @Override public TomatoState getTomato(int userId) { return database.tomatoDao().getTomatoByUserId(userId); }
    @Override public void createInitialTomato(int userId) {
        TomatoState existing = database.tomatoDao().getTomatoByUserId(userId);
        if (existing != null) return;
        TomatoState tomato = new TomatoState(); tomato.setUserId(userId); tomato.setProgress(0); tomato.setStage(0); tomato.setCreatedAt(System.currentTimeMillis()); tomato.setHarvestedCount(0);
        database.tomatoDao().insertTomato(tomato);
    }
    @Override public void updateProgressDirect(int tomatoId, int newProgress, int newStage) { database.tomatoDao().updateProgress(tomatoId, newProgress, newStage, System.currentTimeMillis()); }

    @Override public WaterResult water(int userId, int tomatoId) {
        synchronized (waterLock) {
            TomatoState tomato = database.tomatoDao().getTomatoByUserId(userId);
            if (tomato == null) return new WaterResult(false, "番茄不存在", 0, 0);
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            int currentEnergy = database.energyRecordDao().getTodayNetEnergy(userId, today);
            if (currentEnergy < WATER_ENERGY_COST) return new WaterResult(false, "能量不足，当前能量: " + currentEnergy, tomato.getProgress(), tomato.getStage());
            WateringLog lastWatering = database.wateringLogDao().getLastWatering(userId, tomatoId);
            if (lastWatering != null && (System.currentTimeMillis() - lastWatering.getWateredAt()) < 2000)
                return new WaterResult(true, "重复操作，已跳过", tomato.getProgress(), tomato.getStage());
            long now = System.currentTimeMillis();
            int oldProgress = tomato.getProgress(); int newProgress = Math.min(oldProgress + WATER_PROGRESS_GAIN, 100); int newStage = calculateStage(newProgress);
            database.runInTransaction(() -> {
                EnergyRecord er = new EnergyRecord(); er.setUserId(userId); er.setEnergyValue(WATER_ENERGY_COST); er.setRecordDate(today); er.setSourceType(1); er.setCapped(0); er.setCreatedAt(now);
                database.energyRecordDao().insertEnergyRecord(er);
                database.tomatoDao().updateProgress(tomatoId, newProgress, newStage, now);
                WateringLog wl = new WateringLog(); wl.setUserId(userId); wl.setTomatoId(tomatoId); wl.setEnergyConsumed(WATER_ENERGY_COST); wl.setProgressBefore(oldProgress); wl.setProgressAfter(newProgress); wl.setWateredAt(now); wl.setRequestId(UUID.randomUUID().toString());
                database.wateringLogDao().insertWateringLog(wl);
            });
            return new WaterResult(true, "浇水成功", newProgress, newStage);
        }
    }
    @Override public HarvestResult harvest(int userId, int tomatoId) {
        TomatoState tomato = database.tomatoDao().getTomatoByUserId(userId);
        if (tomato == null) return new HarvestResult(false, "番茄不存在", 0);
        if (tomato.getProgress() < 100) return new HarvestResult(false, "番茄还未成熟", tomato.getHarvestedCount());
        HarvestLog lastHarvest = database.harvestLogDao().getLastHarvest(userId, tomatoId);
        if (lastHarvest != null && (System.currentTimeMillis() - lastHarvest.getHarvestedAt()) < 2000)
            return new HarvestResult(true, "重复操作，已跳过", tomato.getHarvestedCount());
        long now = System.currentTimeMillis(); String requestId = UUID.randomUUID().toString();
        database.runInTransaction(() -> {
            database.tomatoDao().resetAndIncrementHarvest(tomatoId);
            HarvestLog log = new HarvestLog(); log.setUserId(userId); log.setTomatoId(tomatoId); log.setHarvestedAt(now); log.setRequestId(requestId);
            database.harvestLogDao().insertHarvestLog(log);
        });
        TomatoState updated = database.tomatoDao().getTomatoByUserId(userId);
        int count = updated != null ? updated.getHarvestedCount() : 0;
        pushHarvestToCloud(userId, tomatoId, now, requestId);
        return new HarvestResult(true, "收获成功", count);
    }
    private void pushHarvestToCloud(int userId, int tomatoId, long harvestedAt, String requestId) {
        if (!EMASClient.getInstance().isInitialized()) return;
        new Thread(() -> { try { User user = database.userDao().getUserById(userId); if (user == null || user.getShortId() == null) return; JSONObject doc = new JSONObject(); doc.put("shortId", user.getShortId()); doc.put("tomatoId", tomatoId); doc.put("harvestedAt", harvestedAt); doc.put("requestId", requestId); String params = EMASClient.buildDbParams("insertOne", "harvest_logs", doc.toString(), null); EMASClient.getInstance().callFunction("sporty-api", params); } catch (Exception e) {} }).start();
    }
    private static int calculateStage(int progress) { if (progress < 20) return 0; else if (progress < 60) return 1; else if (progress < 100) return 2; else return 3; }
}