package com.sportytomato.app.repository.mock;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sportytomato.app.model.entity.HarvestLog;
import com.sportytomato.app.model.entity.TomatoState;
import com.sportytomato.app.model.entity.WateringLog;
import com.sportytomato.app.repository.TomatoRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TomatoRepository 的 Mock 实现，使用内存存储番茄状态、浇水日志、收获日志。
 * 内部维护独立能量 Map，不与 MockEnergyRepository 共享。
 * 所有方法在关键点 Thread.sleep(300) 模拟网络延迟。
 *
 * 业务规则：
 * - 浇水消耗 100 能量，progress +20，按比例计算 stage
 * - 收获要求 progress == 100，收获后重置 progress 并计数
 * - 枯萎检查：超过 24 小时未浇水且 progress < 100，progress -= 5
 */
public class MockTomatoRepository implements TomatoRepository {

    private static final int WATER_ENERGY_COST = 100;
    private static final int WATER_PROGRESS_GAIN = 20;
    // key = userId
    private final ConcurrentHashMap<Integer, TomatoState> tomatoes = new ConcurrentHashMap<>();
    // 浇水日志列表
    private final List<WateringLog> wateringLogs = new ArrayList<>();
    // 收获日志列表
    private final List<HarvestLog> harvestLogs = new ArrayList<>();
    // 内部能量 Map: key = "userId_date"
    private final ConcurrentHashMap<String, Integer> energyMap = new ConcurrentHashMap<>();
    // tomatoId 自增
    private final AtomicInteger nextTomatoId = new AtomicInteger(1);

    @Override
    public LiveData<TomatoState> getTomatoLive(int userId) {
        simulateNetworkDelay();
        MutableLiveData<TomatoState> liveData = new MutableLiveData<>();
        liveData.setValue(tomatoes.get(userId));
        return liveData;
    }

    @Override
    public TomatoState getTomato(int userId) {
        simulateNetworkDelay();
        return tomatoes.get(userId);
    }

    @Override
    public void createInitialTomato(int userId) {
        simulateNetworkDelay();
        // 已存在则跳过
        if (tomatoes.containsKey(userId)) {
            return;
        }
        long now = System.currentTimeMillis();
        TomatoState tomato = new TomatoState();
        tomato.setTomatoId(nextTomatoId.getAndIncrement());
        tomato.setUserId(userId);
        tomato.setProgress(0);
        tomato.setStage(0);
        tomato.setCreatedAt(now);
        tomato.setLastWateredAt(0);
        tomato.setHarvestedCount(0);
        tomatoes.put(userId, tomato);
    }

    @Override
    public void updateProgressDirect(int tomatoId, int newProgress, int newStage) {
        // Mock 实现：直接更新番茄进度（不扣能量）
        tomatoes.values().stream()
                .filter(t -> t.getTomatoId() == tomatoId)
                .findFirst()
                .ifPresent(t -> {
                    t.setProgress(newProgress);
                    t.setStage(newStage);
                    t.setLastWateredAt(System.currentTimeMillis());
                });
    }

    @Override
    public WaterResult water(int userId, int tomatoId) {
        simulateNetworkDelay();

        String today = getTodayDateStr();
        String energyKey = makeEnergyKey(userId, today);
        int currentEnergy = energyMap.getOrDefault(energyKey, 0);

        // 检查能量
        if (currentEnergy < WATER_ENERGY_COST) {
            TomatoState t = tomatoes.get(userId);
            int p = t != null ? t.getProgress() : 0;
            int s = t != null ? t.getStage() : 0;
            return new WaterResult(false, "能量不足，当前能量: " + currentEnergy, p, s);
        }

        // 检查番茄存在
        TomatoState tomato = tomatoes.get(userId);
        if (tomato == null) {
            return new WaterResult(false, "番茄不存在", 0, 0);
        }

        // 检查是否已成熟
        if (tomato.getProgress() >= 100) {
            return new WaterResult(false, "番茄已成熟，请收获", tomato.getProgress(), tomato.getStage());
        }

        long now = System.currentTimeMillis();

        // 扣减能量
        energyMap.put(energyKey, currentEnergy - WATER_ENERGY_COST);

        // 更新番茄进度
        int oldProgress = tomato.getProgress();
        int newProgress = Math.min(oldProgress + WATER_PROGRESS_GAIN, 100);
        int newStage = calculateStage(newProgress);

        tomato.setProgress(newProgress);
        tomato.setStage(newStage);
        tomato.setLastWateredAt(now);
        tomatoes.put(userId, tomato);

        // 记录浇水日志
        WateringLog log = new WateringLog();
        log.setLogId(wateringLogs.size() + 1);
        log.setUserId(userId);
        log.setTomatoId(tomato.getTomatoId());
        log.setEnergyConsumed(WATER_ENERGY_COST);
        log.setProgressBefore(oldProgress);
        log.setProgressAfter(newProgress);
        log.setWateredAt(now);
        wateringLogs.add(log);

        return new WaterResult(true, "浇水成功", newProgress, newStage);
    }

    @Override
    public HarvestResult harvest(int userId, int tomatoId) {
        simulateNetworkDelay();

        TomatoState tomato = tomatoes.get(userId);
        if (tomato == null) {
            return new HarvestResult(false, "番茄不存在", 0);
        }

        if (tomato.getProgress() < 100) {
            return new HarvestResult(false, "番茄还未成熟", tomato.getHarvestedCount());
        }

        // 重置番茄进度但保留收获计数
        tomato.setProgress(0);
        tomato.setStage(0);
        tomato.setLastWateredAt(0);
        int newHarvestedCount = tomato.getHarvestedCount() + 1;
        tomato.setHarvestedCount(newHarvestedCount);

        long now = System.currentTimeMillis();

        tomatoes.put(userId, tomato);

        // 记录收获日志
        HarvestLog log = new HarvestLog();
        log.setLogId(harvestLogs.size() + 1);
        log.setUserId(userId);
        log.setTomatoId(tomato.getTomatoId());
        log.setHarvestedAt(now);
        harvestLogs.add(log);

        return new HarvestResult(true, "收获成功", newHarvestedCount);
    }

    /**
     * 计算 stage：0-19→0, 20-59→1, 60-99→2, 100→3
     */
    private static int calculateStage(int progress) {
        if (progress < 20) {
            return 0;
        } else if (progress < 60) {
            return 1;
        } else if (progress < 100) {
            return 2;
        } else {
            return 3;
        }
    }

    private String makeEnergyKey(int userId, String date) {
        return userId + "_" + date;
    }

    private String getTodayDateStr() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    /**
     * 供 MockStatsRepository 查询浇水日志列表
     */
    public List<WateringLog> getWateringLogs() {
        return wateringLogs;
    }

    /**
     * 供 MockStatsRepository 查询收获日志列表
     */
    public List<HarvestLog> getHarvestLogs() {
        return harvestLogs;
    }

    /**
     * 供 MockStatsRepository 查询内部能量 Map
     */
    public ConcurrentHashMap<String, Integer> getEnergyMap() {
        return energyMap;
    }

    /**
     * 模拟网络延迟
     */
    private void simulateNetworkDelay() {
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
