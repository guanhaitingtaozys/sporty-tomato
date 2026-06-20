package com.sportytomato.app.service;

import android.content.Context;
import com.sportytomato.app.R;
import com.sportytomato.app.model.database.AppDatabase;
import com.sportytomato.app.model.entity.BadgeTemplate;
import com.sportytomato.app.model.entity.UserBadge;
import java.util.ArrayList;
import java.util.List;

public class AchievementService {
    private final AppDatabase database;
    private final Context context;

    public AchievementService(AppDatabase database, Context context) { this.database = database; this.context = context; }

    public void initBadgeTemplates() {
        List<BadgeTemplate> existing = database.badgeDao().getAllBadges();
        if (!existing.isEmpty()) return;
        BadgeTemplate t;
        t = new BadgeTemplate(); t.setBadgeId(1); t.setBadgeName("初次收获"); t.setDescription("完成首次番茄收获"); t.setIconName("badge_first_harvest"); t.setTriggerType(0); t.setTriggerValue(1); database.badgeDao().insertBadgeTemplate(t);
        t = new BadgeTemplate(); t.setBadgeId(2); t.setBadgeName("收获达人"); t.setDescription("累计收获5次"); t.setIconName("badge_harvest_5"); t.setTriggerType(1); t.setTriggerValue(5); database.badgeDao().insertBadgeTemplate(t);
        t = new BadgeTemplate(); t.setBadgeId(3); t.setBadgeName("收获大师"); t.setDescription("累计收获10次"); t.setIconName("badge_harvest_10"); t.setTriggerType(2); t.setTriggerValue(10); database.badgeDao().insertBadgeTemplate(t);
        t = new BadgeTemplate(); t.setBadgeId(4); t.setBadgeName("园丁"); t.setDescription("累计浇水10次"); t.setIconName("badge_water_10"); t.setTriggerType(3); t.setTriggerValue(10); database.badgeDao().insertBadgeTemplate(t);
        t = new BadgeTemplate(); t.setBadgeId(5); t.setBadgeName("万步达人"); t.setDescription("单日步数突破10000"); t.setIconName("badge_steps_10000"); t.setTriggerType(4); t.setTriggerValue(10000); database.badgeDao().insertBadgeTemplate(t);
        t = new BadgeTemplate(); t.setBadgeId(6); t.setBadgeName("能量大师"); t.setDescription("累计获得能量突破500"); t.setIconName("badge_energy_500"); t.setTriggerType(5); t.setTriggerValue(500); database.badgeDao().insertBadgeTemplate(t);
        t = new BadgeTemplate(); t.setBadgeId(7); t.setBadgeName("好友达人"); t.setDescription("拥有5位以上好友"); t.setIconName("badge_friends_5"); t.setTriggerType(6); t.setTriggerValue(5); database.badgeDao().insertBadgeTemplate(t);
    }

    public boolean checkAndUnlock(int userId, int triggerType, int currentValue) {
        List<BadgeTemplate> templates = database.badgeDao().getAllBadges();
        boolean unlocked = false;
        for (BadgeTemplate template : templates) {
            if (template.getTriggerType() == triggerType && currentValue >= template.getTriggerValue()) {
                UserBadge existing = database.badgeDao().getUserBadge(userId, template.getBadgeId());
                if (existing == null) {
                    UserBadge newBadge = new UserBadge(); newBadge.setUserId(userId); newBadge.setBadgeId(template.getBadgeId()); newBadge.setUnlockedAt(System.currentTimeMillis());
                    database.badgeDao().insertBadge(newBadge);
                    markBadgeAsNew(userId, template.getBadgeId());
                    unlocked = true;
                }
            }
        }
        return unlocked;
    }

    public List<BadgeTemplate> getUnlockedBadges(int userId) {
        List<UserBadge> userBadges = database.badgeDao().getUserBadges(userId);
        List<BadgeTemplate> allTemplates = database.badgeDao().getAllBadges();
        List<BadgeTemplate> result = new ArrayList<>();
        for (UserBadge ub : userBadges) { for (BadgeTemplate t : allTemplates) { if (t.getBadgeId() == ub.getBadgeId()) result.add(t); } }
        return result;
    }

    public void markBadgeAsNew(int userId, int badgeId) { context.getSharedPreferences("badge_new_prefs", Context.MODE_PRIVATE).edit().putBoolean("badge_new_" + userId + "_" + badgeId, true).apply(); }
    public void clearBadgeNewFlag(int userId, int badgeId) { context.getSharedPreferences("badge_new_prefs", Context.MODE_PRIVATE).edit().putBoolean("badge_new_" + userId + "_" + badgeId, false).apply(); }
    public boolean isBadgeNew(int userId, int badgeId) { return context.getSharedPreferences("badge_new_prefs", Context.MODE_PRIVATE).getBoolean("badge_new_" + userId + "_" + badgeId, false); }

    public List<BadgeWithStatus> getAllBadgesWithStatus(int userId) {
        List<BadgeTemplate> allTemplates = database.badgeDao().getAllBadges();
        List<UserBadge> userBadges = database.badgeDao().getUserBadges(userId);
        List<BadgeWithStatus> result = new ArrayList<>();
        for (BadgeTemplate t : allTemplates) {
            BadgeWithStatus item = new BadgeWithStatus(); item.template = t; item.unlocked = false; item.unlockedAt = 0;
            for (UserBadge ub : userBadges) { if (ub.getBadgeId() == t.getBadgeId()) { item.unlocked = true; item.unlockedAt = ub.getUnlockedAt(); break; } }
            result.add(item);
        }
        return result;
    }

    public static class BadgeWithStatus { public BadgeTemplate template; public boolean unlocked; public long unlockedAt; }

    public static int getBadgeIconRes(String iconName) {
        if (iconName == null) return 0;
        switch (iconName) {
            case "badge_first_harvest": return R.drawable.badge_first_harvest;
            case "badge_harvest_5": return R.drawable.badge_harvest_5;
            case "badge_harvest_10": return R.drawable.badge_harvest_10;
            case "badge_water_10": return R.drawable.badge_water_10;
            case "badge_steps_10000": return R.drawable.badge_steps_10000;
            case "badge_energy_500": return R.drawable.badge_energy_500;
            case "badge_friends_5": return R.drawable.badge_friends_5;
            default: return 0;
        }
    }
}