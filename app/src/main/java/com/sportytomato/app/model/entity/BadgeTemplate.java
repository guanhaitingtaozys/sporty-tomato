package com.sportytomato.app.model.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "badge_templates")
public class BadgeTemplate {
    @PrimaryKey private int badgeId;
    private String badgeName;
    private String description;
    private String iconName;
    private int triggerType;
    private int triggerValue;
    public BadgeTemplate() {}
    public int getBadgeId() { return badgeId; }
    public void setBadgeId(int badgeId) { this.badgeId = badgeId; }
    public String getBadgeName() { return badgeName; }
    public void setBadgeName(String badgeName) { this.badgeName = badgeName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIconName() { return iconName; }
    public void setIconName(String iconName) { this.iconName = iconName; }
    public int getTriggerType() { return triggerType; }
    public void setTriggerType(int triggerType) { this.triggerType = triggerType; }
    public int getTriggerValue() { return triggerValue; }
    public void setTriggerValue(int triggerValue) { this.triggerValue = triggerValue; }
}