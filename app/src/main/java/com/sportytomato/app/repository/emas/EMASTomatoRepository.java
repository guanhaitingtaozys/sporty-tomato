package com.sportytomato.app.repository.emas;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sportytomato.app.model.entity.TomatoState;
import com.sportytomato.app.repository.TomatoRepository;
import com.sportytomato.app.service.EMASClient;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EMASTomatoRepository implements TomatoRepository {

    private static final String COLLECTION = "tomato_states";
    private static final String FUNCTION_NAME = "sporty-api";

    private final EMASClient client;
    private final ExecutorService executor;
    private final MutableLiveData<TomatoState> tomatoLive = new MutableLiveData<>();

    public EMASTomatoRepository() {
        this.client = EMASClient.getInstance();
        this.executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public LiveData<TomatoState> getTomatoLive(int userId) {
        executor.execute(() -> {
            TomatoState tomato = getTomato(userId);
            tomatoLive.postValue(tomato);
        });
        return tomatoLive;
    }

    @Override
    public TomatoState getTomato(int userId) {
        try {
            JSONObject query = new JSONObject();
            query.put("userId", userId);

            String params = EMASClient.buildDbParams("findOne", COLLECTION,
                    null, query.toString());
            String response = client.callFunction(FUNCTION_NAME, params);

            if (response != null) {
                JSONObject root = new JSONObject(response);
                JSONObject resObj = root.optJSONObject("result");
                if (resObj != null) {
                    return parseTomatoJson(resObj);
                }
                // Try direct result
                return parseTomatoJson(root);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void createInitialTomato(int userId) {
        try {
            JSONObject data = new JSONObject();
            data.put("userId", userId);
            data.put("stage", 0);
            data.put("progress", 0);
            data.put("harvestedCount", 0);
            data.put("lastWateredAt", System.currentTimeMillis());
            data.put("createdAt", System.currentTimeMillis());

            String params = EMASClient.buildDbParams("insertOne", COLLECTION,
                    data.toString(), null);
            client.callFunction(FUNCTION_NAME, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateProgressDirect(int tomatoId, int newProgress, int newStage) {
        // EMAS 实现暂不处理（帮浇水为本地功能）
    }

    @Override
    public WaterResult water(int userId, int tomatoId) {
        try {
            TomatoState tomato = getTomato(userId);
            if (tomato == null) {
                return new WaterResult(false, "番茄不存在", 0, 0);
            }
            if (tomato.getProgress() >= 100) {
                return new WaterResult(false, "番茄已成熟，请收获", tomato.getProgress(), tomato.getStage());
            }

            int newProgress = Math.min(100, tomato.getProgress() + 20);
            int newStage = tomato.getStage();
                if (newProgress >= 20 && newStage < 1) newStage = 1;
                else if (newProgress >= 60 && newStage < 2) newStage = 2;
                else if (newProgress >= 100 && newStage < 3) newStage = 3;

            JSONObject query = new JSONObject();
            query.put("tomatoId", tomatoId);
            query.put("userId", userId);

            JSONObject update = new JSONObject();
            update.put("progress", newProgress);
            update.put("stage", newStage);
            update.put("lastWateredAt", System.currentTimeMillis());

            String params = EMASClient.buildDbParams("updateOne", COLLECTION,
                    update.toString(), query.toString());
            String response = client.callFunction(FUNCTION_NAME, params);

            if (response != null) {
                // Also record watering in water_records
                JSONObject waterData = new JSONObject();
                waterData.put("userId", userId);
                waterData.put("tomatoId", tomatoId);
                waterData.put("wateredAt", System.currentTimeMillis());
                String waterParams = EMASClient.buildDbParams("insertOne", "watering_logs",
                        waterData.toString(), null);
                client.callFunction(FUNCTION_NAME, waterParams);

                return new WaterResult(true, "浇水成功！+" + (newProgress - tomato.getProgress()) + "%", newProgress, newStage);
            }
            return new WaterResult(false, "浇水失败，请重试", tomato.getProgress(), tomato.getStage());
        } catch (Exception e) {
            e.printStackTrace();
            return new WaterResult(false, "浇水发生错误", 0, 0);
        }
    }

    @Override
    public HarvestResult harvest(int userId, int tomatoId) {
        try {
            TomatoState tomato = getTomato(userId);
            if (tomato == null) {
                return new HarvestResult(false, "番茄不存在", 0);
            }
            if (tomato.getProgress() < 100) {
                return new HarvestResult(false, "番茄尚未成熟", tomato.getHarvestedCount());
            }

            // Reset progress for next cycle
            int newHarvestCount = tomato.getHarvestedCount() + 1;

            JSONObject query = new JSONObject();
            query.put("tomatoId", tomatoId);
            query.put("userId", userId);

            JSONObject update = new JSONObject();
            update.put("progress", 0);
            update.put("stage", 0);
            update.put("harvestedCount", newHarvestCount);
            update.put("lastWateredAt", System.currentTimeMillis());

            String params = EMASClient.buildDbParams("updateOne", COLLECTION,
                    update.toString(), query.toString());
            String response = client.callFunction(FUNCTION_NAME, params);

            if (response != null) {
                return new HarvestResult(true, "收获成功！累计收获 " + newHarvestCount + " 个", newHarvestCount);
            }
            return new HarvestResult(false, "收获失败，请重试", tomato.getHarvestedCount());
        } catch (Exception e) {
            e.printStackTrace();
            return new HarvestResult(false, "收获发生错误", 0);
        }
    }

    private TomatoState parseTomatoJson(JSONObject json) {
        try {
            TomatoState tomato = new TomatoState();
            tomato.setTomatoId(json.optInt("tomatoId", json.optInt("_id", 0)));
            tomato.setUserId(json.optInt("userId", 0));
            tomato.setStage(json.optInt("stage", 0));
            tomato.setProgress(json.optInt("progress", 0));
            tomato.setHarvestedCount(json.optInt("harvestedCount", 0));
            tomato.setLastWateredAt(json.optLong("lastWateredAt", System.currentTimeMillis()));
            tomato.setCreatedAt(json.optLong("createdAt", System.currentTimeMillis()));
            return tomato;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
