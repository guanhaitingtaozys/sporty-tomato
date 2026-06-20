package com.sportytomato.app.repository.mock;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sportytomato.app.model.entity.HarvestLog;
import com.sportytomato.app.repository.HarvestRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * HarvestRepository 的 Mock 实现，使用内存 List 存储收获日志。
 * 所有方法在关键点 Thread.sleep(300) 模拟网络延迟。
 */
public class MockHarvestRepository implements HarvestRepository {

    // 收获日志列表
    private final List<HarvestLog> harvestLogs = new ArrayList<>();

    @Override
    public int getTotalHarvestCount(int userId) {
        simulateNetworkDelay();
        int count = 0;
        for (HarvestLog log : harvestLogs) {
            if (log.getUserId() == userId) {
                count++;
            }
        }
        return count;
    }

    @Override
    public List<HarvestLog> getHarvestHistory(int userId) {
        simulateNetworkDelay();

        // 筛选当前用户的收获日志
        List<HarvestLog> userLogs = new ArrayList<>();
        for (HarvestLog log : harvestLogs) {
            if (log.getUserId() == userId) {
                userLogs.add(log);
            }
        }

        // 按时间倒序排列
        Collections.sort(userLogs, new Comparator<HarvestLog>() {
            @Override
            public int compare(HarvestLog a, HarvestLog b) {
                return Long.compare(b.getHarvestedAt(), a.getHarvestedAt());
            }
        });

        // 最多返回 50 条
        if (userLogs.size() > 50) {
            return userLogs.subList(0, 50);
        }
        return userLogs;
    }

    @Override
    public LiveData<Integer> getTotalHarvestCountLive(int userId) {
        simulateNetworkDelay();
        MutableLiveData<Integer> liveData = new MutableLiveData<>();
        liveData.setValue(getTotalHarvestCount(userId));
        return liveData;
    }

    @Override
    public HarvestLog getByRequestId(String requestId) {
        simulateNetworkDelay();
        for (HarvestLog log : harvestLogs) {
            if (requestId != null && requestId.equals(log.getRequestId())) {
                return log;
            }
        }
        return null;
    }

    @Override
    public void insertHarvestLog(HarvestLog log) {
        harvestLogs.add(log);
    }

    /**
     * 添加收获日志（供 MockTomatoRepository 调用）
     */
    public void addHarvestLog(HarvestLog log) {
        harvestLogs.add(log);
    }

    @Override
    public int getPeriodHarvestCount(int userId, long startTime, long endTime) {
        simulateNetworkDelay();
        int count = 0;
        for (HarvestLog log : harvestLogs) {
            if (log.getUserId() == userId
                    && log.getHarvestedAt() >= startTime
                    && log.getHarvestedAt() <= endTime) {
                count++;
            }
        }
        return count;
    }

    /**
     * 获取全部日志列表（供 MockStatsRepository 查询）
     */
    public List<HarvestLog> getHarvestLogs() {
        return harvestLogs;
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
