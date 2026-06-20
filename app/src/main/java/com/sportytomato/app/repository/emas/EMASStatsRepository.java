package com.sportytomato.app.repository.emas;

import com.sportytomato.app.repository.StatsRepository;
import com.sportytomato.app.service.EMASClient;

import org.json.JSONArray;
import org.json.JSONObject;

public class EMASStatsRepository implements StatsRepository {

    private static final String FUNCTION_NAME = "sporty-api";
    private final EMASClient client;

    public EMASStatsRepository() {
        this.client = EMASClient.getInstance();
    }

    @Override
    public int getTodayWateringCount(int userId, String date) {
        try {
            JSONObject query = new JSONObject();
            query.put("userId", userId);
            query.put("wateredAt", new JSONObject().put("$gte", date + "T00:00:00.000Z"));
            String params = EMASClient.buildDbParams("count", "watering_logs", null, query.toString());
            String response = client.callFunction(FUNCTION_NAME, params);
            if (response != null) {
                JSONObject root = new JSONObject(response);
                JSONObject resObj = root.optJSONObject("result");
                if (resObj != null) return resObj.optInt("total", 0);
                return root.optInt("total", 0);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    @Override
    public int getTotalHarvestCount(int userId) {
        try {
            JSONObject query = new JSONObject();
            query.put("userId", userId);
            String params = EMASClient.buildDbParams("count", "harvest_logs", null, query.toString());
            String response = client.callFunction(FUNCTION_NAME, params);
            if (response != null) {
                JSONObject root = new JSONObject(response);
                JSONObject resObj = root.optJSONObject("result");
                if (resObj != null) return resObj.optInt("total", 0);
                return root.optInt("total", 0);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    @Override
    public int getTodayEnergy(int userId, String date) {
        try {
            JSONObject query = new JSONObject();
            query.put("userId", userId);
            query.put("recordDate", date);
            String params = EMASClient.buildDbParams("find", "energy_records", null, query.toString());
            String response = client.callFunction(FUNCTION_NAME, params);
            if (response != null) {
                JSONObject root = new JSONObject(response);
                int total = 0;
                if (root.has("result")) {
                    Object res = root.get("result");
                    JSONArray arr = null;
                    if (res instanceof JSONArray) {
                        arr = (JSONArray) res;
                    } else if (res instanceof JSONObject && ((JSONObject) res).has("data")) {
                        arr = ((JSONObject) res).optJSONArray("data");
                    }
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject record = arr.getJSONObject(i);
                            int sourceType = record.optInt("sourceType", 0);
                            int value = record.optInt("energyValue", 0);
                            if (sourceType == 0) total += value;
                            else total -= Math.abs(value);
                        }
                    }
                }
                return total;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }
}
