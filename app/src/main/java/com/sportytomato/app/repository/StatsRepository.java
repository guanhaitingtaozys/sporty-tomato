package com.sportytomato.app.repository;

public interface StatsRepository {
    int getTodayWateringCount(int userId, String date);
    int getTotalHarvestCount(int userId);
    int getTodayEnergy(int userId, String date);
}