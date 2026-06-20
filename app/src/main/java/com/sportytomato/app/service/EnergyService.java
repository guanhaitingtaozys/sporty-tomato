package com.sportytomato.app.service;

import com.sportytomato.app.model.database.AppDatabase;
import com.sportytomato.app.repository.EnergyRepository;
import com.sportytomato.app.repository.EnergyRepositoryImpl;

public class EnergyService {
    private final EnergyRepository energyRepository;
    public EnergyService(AppDatabase database) { this.energyRepository = new EnergyRepositoryImpl(database); }
    public void addManualSteps(int userId, int steps) { energyRepository.addSteps(userId, steps); }
    public int getTodaySteps(int userId) { return energyRepository.getTodaySteps(userId); }
    public int getTodayEnergy(int userId) { return energyRepository.getTodayEnergy(userId); }
    public String getTodayDate() { return energyRepository.getTodayDate(); }
}