package com.sportytomato.app.repository;

public interface EnergyRepository {
    int getTodayEnergy(int userId);
    int getTodayStepEnergy(int userId);
    void addSteps(int userId, int steps);
    void addStepsSync(int userId, int steps);
    void consumeEnergy(int userId, int amount);
    void consumeEnergyWithType(int userId, int amount, int sourceType);
    int getTodaySteps(int userId);
    int getDailyNetEnergy(int userId, String date);
    int getEnergyBySourceType(int userId, String date, int sourceType);
    String getTodayDate();
}