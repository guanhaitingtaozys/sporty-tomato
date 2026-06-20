package com.sportytomato.app.repository.mock;

import com.sportytomato.app.model.entity.HarvestLog;
import com.sportytomato.app.model.entity.WateringLog;
import com.sportytomato.app.repository.StatsRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * StatsRepository 的 Mock 实现，从 Mock 层内存中直接获取统计数据。
 * 所有方法在关键点 Thread.sleep(300) 模拟网络延迟。
 */
public class MockStatsRepository implements StatsRepository {

    // 引用其他 Mock 仓库，用于获取底层数据
    private final MockTomatoRepository mockTomatoRepo;
    private final MockHarvestRepository mockHarvestRepo;

    public MockStatsRepository(MockTomatoRepository mockTomatoRepo,
                                MockHarvestRepository mockHarvestRepo) {
        this.mockTomatoRepo = mockTomatoRepo;
        this.mockHarvestRepo = mockHarvestRepo;
    }

    @Override
    public int getTodayWateringCount(int userId, String date) {
        simulateNetworkDelay();

        List<WateringLog> logs = mockTomatoRepo.getWateringLogs();
        String datePrefix = date; // 格式 "yyyy-MM-dd"
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        int count = 0;
        for (WateringLog log : logs) {
            if (log.getUserId() == userId) {
                String logDate = sdf.format(new Date(log.getWateredAt()));
                if (datePrefix.equals(logDate)) {
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    public int getTotalHarvestCount(int userId) {
        simulateNetworkDelay();

        List<HarvestLog> logs = mockHarvestRepo.getHarvestLogs();
        int count = 0;
        for (HarvestLog log : logs) {
            if (log.getUserId() == userId) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int getTodayEnergy(int userId, String date) {
        simulateNetworkDelay();

        ConcurrentHashMap<String, Integer> energyMap = mockTomatoRepo.getEnergyMap();
        String key = userId + "_" + date;
        Integer energy = energyMap.get(key);
        return energy != null ? energy : 0;
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
