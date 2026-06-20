package com.sportytomato.app.service;

public class TomatoService {
    private TomatoService() {}
    public static String getStageName(int stage) { switch (stage) { case 0: return "种子阶段"; case 1: return "小苗阶段"; case 2: return "开花阶段"; case 3: return "成果阶段"; default: return "未知阶段"; } }
    public static String getStageIcon(int stage) { switch (stage) { case 0: return "seed"; case 1: return "sprout"; case 2: return "bloom"; case 3: return "mature"; default: return "seed"; } }
    public static int calculateStage(int progress) { if (progress < 20) return 0; else if (progress < 60) return 1; else if (progress < 100) return 2; else return 3; }
}