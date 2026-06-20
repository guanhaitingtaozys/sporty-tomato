package com.sportytomato.app.service;

import java.util.Random;

public class HarvestService {
    private static final Random RANDOM = new Random();
    private static final String[] DONATE_MESSAGES = {
        "你的番茄已经捐赠给「山区儿童营养计划」，感谢你的善心！每一份收获都传递着温暖。",
        "恭喜！番茄已送往「流浪动物救助站」，小动物们谢谢你～",
        "这颗番茄将加入「城市绿化基金」，为地球添一抹绿色！",
        "你的番茄分享给了「乡村图书馆」，知识因你而传递！",
        "番茄化身「爱心午餐」，为需要帮助的人送去一顿温暖。",
        "这颗番茄变成了「环保种子」，将在各地播撒绿色希望！",
        "你的努力化作了「助学基金」，帮助一个孩子离梦想更近一步。"
    };
    private HarvestService() {}
    public static String getRandomDonateMessage() { return DONATE_MESSAGES[RANDOM.nextInt(DONATE_MESSAGES.length)]; }
}