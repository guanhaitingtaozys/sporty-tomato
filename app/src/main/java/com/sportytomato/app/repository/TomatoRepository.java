package com.sportytomato.app.repository;

import androidx.lifecycle.LiveData;
import com.sportytomato.app.model.entity.TomatoState;

public interface TomatoRepository {
    LiveData<TomatoState> getTomatoLive(int userId);
    TomatoState getTomato(int userId);
    void createInitialTomato(int userId);
    WaterResult water(int userId, int tomatoId);
    void updateProgressDirect(int tomatoId, int newProgress, int newStage);
    HarvestResult harvest(int userId, int tomatoId);
    class WaterResult { public boolean success; public String message; public int newProgress; public int newStage; public WaterResult(boolean success, String message, int newProgress, int newStage) { this.success = success; this.message = message; this.newProgress = newProgress; this.newStage = newStage; } }
    class HarvestResult { public boolean success; public String message; public int harvestCount; public HarvestResult(boolean success, String message, int harvestCount) { this.success = success; this.message = message; this.harvestCount = harvestCount; } }
}