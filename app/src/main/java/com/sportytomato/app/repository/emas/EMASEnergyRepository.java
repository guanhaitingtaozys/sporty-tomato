package com.sportytomato.app.repository.emas;

import com.sportytomato.app.repository.EnergyRepository;
import com.sportytomato.app.service.EMASClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * EMAS Serverless 实现的 EnergyRepository
 */
public class EMASEnergyRepository implements EnergyRepository {

    private static final String COLLECTION = "energy_records";
    private static final String FUNCTION_NAME = "sporty-api";
    private static final int DAILY_ENERGY_CAP = 2000;

    private final EMASClient client;
    private final ExecutorService executor;

    public EMASEnergyRepository() {
        this.client = EMASClient.getInstance();
        this.executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public int getTodayEnergy(int userId) {
        int total = 0;
        try {
            String todayDate = getTodayDate();

            JSONObject query = new JSONObject();
            query.put("userId", userId);
            query.put("recordDate", todayDate);

            String params = EMASClient.buildDbParamsWithOptions("find", COLLECTION,
                    null, query.toString(), null);
            String response = client.callFunction(FUNCTION_NAME, params);

            if (response != null) {
                JSONArray arr = parseResultArray(response);
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject record = arr.getJSONObject(i);
                        int sourceType = record.optInt("sourceType", 0);
                        int value = record.optInt("energyValue", 0);
                        if (sourceType == 0) {
                            total += value;
                        } else {
                            total -= Math.abs(value);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return total;
    }

    @Override
    public int getTodayStepEnergy(int userId) {
        return getTodayEnergy(userId);
    }

    @Override
    public void addSteps(int userId, int steps) {
        executor.execute(() -> addStepsInternal(userId, steps));
    }

    @Override
    public void addStepsSync(int userId, int steps) {
        addStepsInternal(userId, steps);
    }

    private void addStepsInternal(int userId, int steps) {
        try {
            int energyValue = steps / 10;
            if (energyValue <= 0) return;

            String todayDate = getTodayDate();

            // 检查每日能量上限
            int currentEnergy = getTodayEnergy(userId);
            if (currentEnergy >= DAILY_ENERGY_CAP) return;

            int newEnergy = currentEnergy + energyValue;
            int capped = 0;
            if (newEnergy > DAILY_ENERGY_CAP) {
                energyValue = DAILY_ENERGY_CAP - currentEnergy;
                capped = 1;
            }

            JSONObject data = new JSONObject();
            data.put("userId", userId);
            data.put("energyValue", energyValue);
            data.put("recordDate", todayDate);
            data.put("sourceType", 0);
            data.put("capped", capped);
            data.put("createdAt", System.currentTimeMillis());

            String params = EMASClient.buildDbParams("insertOne", COLLECTION,
                    data.toString(), null);
            client.callFunction(FUNCTION_NAME, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void consumeEnergy(int userId, int amount) {
        consumeEnergyWithType(userId, amount, 1);
    }

    @Override
    public void consumeEnergyWithType(int userId, int amount, int sourceType) {
        executor.execute(() -> {
            try {
                String todayDate = getTodayDate();

                int currentEnergy = getDailyNetEnergy(userId, todayDate);
                if (currentEnergy < amount) {
                    android.util.Log.w("EMASEnergy", "能量不足: 当前=" + currentEnergy + ", 需要=" + amount);
                    return;
                }

                JSONObject data = new JSONObject();
                data.put("userId", userId);
                data.put("energyValue", -amount);
                data.put("recordDate", todayDate);
                data.put("sourceType", sourceType);
                data.put("capped", 0);
                data.put("createdAt", System.currentTimeMillis());

                String params = EMASClient.buildDbParams("insertOne", COLLECTION,
                        data.toString(), null);
                client.callFunction(FUNCTION_NAME, params);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public int getTodaySteps(int userId) {
        try {
            String todayDate = getTodayDate();

            JSONObject query = new JSONObject();
            query.put("userId", userId);
            query.put("recordDate", todayDate);
            query.put("sourceType", 0);

            String params = EMASClient.buildDbParamsWithOptions("find", COLLECTION,
                    null, query.toString(), null);
            String response = client.callFunction(FUNCTION_NAME, params);

            if (response != null) {
                JSONArray arr = parseResultArray(response);
                if (arr != null && arr.length() > 0) {
                    JSONObject record = arr.getJSONObject(arr.length() - 1);
                    int energyValue = record.optInt("energyValue", 0);
                    return energyValue * 10;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    @Override
    public int getDailyNetEnergy(int userId, String date) {
        int total = 0;
        try {
            JSONObject query = new JSONObject();
            query.put("userId", userId);
            query.put("recordDate", date);

            String params = EMASClient.buildDbParamsWithOptions("find", COLLECTION,
                    null, query.toString(), null);
            String response = client.callFunction(FUNCTION_NAME, params);

            if (response != null) {
                JSONArray arr = parseResultArray(response);
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject record = arr.getJSONObject(i);
                        int sourceType = record.optInt("sourceType", 0);
                        int value = record.optInt("energyValue", 0);
                        if (sourceType == 0) {
                            total += value;
                        } else {
                            total -= Math.abs(value);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return total;
    }

    @Override
    public int getEnergyBySourceType(int userId, String date, int sourceType) {
        int total = 0;
        try {
            JSONObject query = new JSONObject();
            query.put("userId", userId);
            query.put("recordDate", date);
            query.put("sourceType", sourceType);

            String params = EMASClient.buildDbParamsWithOptions("find", COLLECTION,
                    null, query.toString(), null);
            String response = client.callFunction(FUNCTION_NAME, params);

            if (response != null) {
                JSONArray arr = parseResultArray(response);
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject record = arr.getJSONObject(i);
                        total += record.optInt("energyValue", 0);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return total;
    }

    // ========== 工具方法 ==========

    private JSONArray parseResultArray(String result) {
        try {
            JSONObject root = new JSONObject(result);
            if (root.has("result")) {
                Object res = root.get("result");
                if (res instanceof JSONArray) {
                    return (JSONArray) res;
                } else if (res instanceof JSONObject) {
                    JSONObject resObj = (JSONObject) res;
                    if (resObj.has("data")) {
                        return resObj.getJSONArray("data");
                    }
                }
            }
            if (root.has("data")) {
                Object data = root.get("data");
                if (data instanceof JSONArray) return (JSONArray) data;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
