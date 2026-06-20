package com.sportytomato.app.service;

import com.sportytomato.app.repository.EnergyRepository;
import java.util.Random;

public class DonateService {
    private static final Random RANDOM = new Random();
    private static final String[] DONATE_MESSAGES = {
        "你的番茄已经捐赠给「山区儿童营养计划」", "恭喜！番茄已送往「流浪动物救助站」",
        "这颗番茄将加入「城市绿化基金」", "你的番茄分享给了「乡村图书馆」",
        "番茄化身「爱心午餐」", "这颗番茄变成了「环保种子」", "你的努力化作了「助学基金」"
    };
    private static final String[] ENCOURAGE_MESSAGES = {
        "每一颗番茄都是一份爱心", "你的能量可以变成他人的温暖", "小小的善举，传递大大的温暖！", "今天又收获了一颗番茄，要不要分享一份爱心？"
    };
    public DonateService() {}
    public boolean shouldShowDonate() { return RANDOM.nextDouble() < 0.3; }
    public static String getRandomDonateMessage() { return DONATE_MESSAGES[RANDOM.nextInt(DONATE_MESSAGES.length)]; }
    public static String getRandomEncourageMessage() { return ENCOURAGE_MESSAGES[RANDOM.nextInt(ENCOURAGE_MESSAGES.length)]; }
    public String donate(int userId, int energyAmount) {
        if (energyAmount <= 0) return "捐赠能量必须大于0";
        EnergyRepository energyRepo = RepositoryManager.getInstance().getEnergyRepo();
        int currentEnergy = energyRepo.getTodayEnergy(userId);
        if (currentEnergy < energyAmount) return "能量不足，无法捐赠。当前能量: " + currentEnergy + "，需要: " + energyAmount;
        energyRepo.consumeEnergy(userId, energyAmount); return getRandomDonateMessage();
    }
    public DonateResult donateWithResult(int userId, int energyAmount) {
        EnergyRepository energyRepo = RepositoryManager.getInstance().getEnergyRepo();
        DonateResult result = new DonateResult();
        if (energyAmount <= 0) { result.success = false; result.message = "捐赠能量必须大于0"; result.remainingEnergy = energyRepo.getTodayEnergy(userId); return result; }
        int currentEnergy = energyRepo.getTodayEnergy(userId);
        if (currentEnergy < energyAmount) { result.success = false; result.message = "能量不足"; result.remainingEnergy = currentEnergy; return result; }
        energyRepo.consumeEnergy(userId, energyAmount);
        result.success = true; result.message = getRandomDonateMessage(); result.remainingEnergy = currentEnergy - energyAmount; result.donatedAmount = energyAmount;
        return result;
    }
    public static class DonateResult { public boolean success; public String message; public int remainingEnergy; public int donatedAmount; }
}