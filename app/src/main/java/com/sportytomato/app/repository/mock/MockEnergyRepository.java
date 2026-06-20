package com.sportytomato.app.repository.mock;

import com.sportytomato.app.repository.EnergyRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EnergyRepository 的 Mock 实现，使用内存 Map 存储每日步数与能量数据。
 * 所有方法在关键点 Thread.sleep(300) 模拟网络延迟。
 * 业务规则：10步=1能量，日能量上限1000。
 */
public class MockEnergyRepository implements EnergyRepository {

    // 每日步数 Map: key = "userId_date"
    private final Map<String, Integer> dailySteps = new ConcurrentHashMap<>();
    // 每日能量 Map: key = "userId_date"
    private final Map<String, Integer> dailyEnergy = new ConcurrentHashMap<>();

    @Override
    public int getTodayEnergy(int userId) {
        simulateNetworkDelay();
        String key = makeKey(userId, getTodayDateInternal());
        Integer energy = dailyEnergy.get(key);
        return energy != null ? energy : 0;
    }

    @Override
    public int getTodayStepEnergy(int userId) {
        return getTodayEnergy(userId);
    }

    @Override
    public void addSteps(int userId, int steps) {
        simulateNetworkDelay();
        String today = getTodayDateInternal();
        String key = makeKey(userId, today);

        // 累加当日步数
        int currentSteps = dailySteps.getOrDefault(key, 0);
        dailySteps.put(key, currentSteps + steps);

        // 计算能量：10步 = 1能量，上限1000
        int currentEnergy = dailyEnergy.getOrDefault(key, 0);
        int energyGain = steps / 10;

        if (energyGain > 0) {
            int newEnergy = currentEnergy + energyGain;
            if (newEnergy > 1000) {
                newEnergy = 1000;
            }
            dailyEnergy.put(key, newEnergy);
        }
    }

    @Override
        public void consumeEnergy(int userId, int amount) {
        consumeEnergyWithType(userId, amount, 1);
    }

    @Override
    public void consumeEnergyWithType(int userId, int amount, int sourceType) {
        simulateNetworkDelay();
        String key = makeKey(userId, getTodayDateInternal());
        int currentEnergy = dailyEnergy.getOrDefault(key, 0);
        if (currentEnergy < amount) {
            return;
        }
        dailyEnergy.put(key, currentEnergy - amount);
    }

    @Override
    public void addStepsSync(int userId, int steps) {
        addSteps(userId, steps);
    }

    @Override
    public int getTodaySteps(int userId) {
        simulateNetworkDelay();
        String key = makeKey(userId, getTodayDateInternal());
        Integer steps = dailySteps.get(key);
        return steps != null ? steps : 0;
    }

    @Override
    public String getTodayDate() {
        simulateNetworkDelay();
        return getTodayDateInternal();
    }

    @Override
    public int getDailyNetEnergy(int userId, String date) {
        simulateNetworkDelay();
        String key = makeKey(userId, date);
        Integer energy = dailyEnergy.get(key);
        return energy != null ? energy : 0;
    }

    @Override
    public int getEnergyBySourceType(int userId, String date, int sourceType) {
        simulateNetworkDelay();
        // Mock 实现：按总能量一半分摊到步数转换（sourceType=0），一半到浇水消耗（sourceType=1）
        int total = getDailyNetEnergy(userId, date);
        if (sourceType == 0) return total / 2;
        // sourceType=1 doesn't really make sense for mock but we return half
        return Math.max(0, total - total / 2);
    }

    // ---- 内部辅助方法 ----

    /**
     * 构造内部 Map 的 key
     */
    private String makeKey(int userId, String date) {
        return userId + "_" + date;
    }

    /**
     * 获取今日日期字符串，无需延时
     */
    private String getTodayDateInternal() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
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
