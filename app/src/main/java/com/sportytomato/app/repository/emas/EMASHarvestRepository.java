package com.sportytomato.app.repository.emas;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sportytomato.app.model.entity.HarvestLog;
import com.sportytomato.app.repository.HarvestRepository;
import com.sportytomato.app.service.EMASClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * EMAS Serverless 实现的 HarvestRepository
 */
public class EMASHarvestRepository implements HarvestRepository {

    private static final String COLLECTION = "harvest_logs";
    private static final String FUNCTION_NAME = "sporty-api";

    private final EMASClient client;
    private final ExecutorService executor;
    private final MutableLiveData<Integer> totalHarvestCountLive = new MutableLiveData<>();

    public EMASHarvestRepository() {
        this.client = EMASClient.getInstance();
        this.executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public int getTotalHarvestCount(int userId) {
        try {
            JSONObject query = new JSONObject();
            query.put("userId", userId);

            String params = EMASClient.buildDbParams("count", COLLECTION,
                    null, query.toString());
            String response = client.callFunction(FUNCTION_NAME, params);

            if (response != null) {
                JSONObject root = new JSONObject(response);
                JSONObject resObj = root.optJSONObject("result");
                if (resObj != null) {
                    return resObj.optInt("total", 0);
                }
                return root.optInt("total", 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public List<HarvestLog> getHarvestHistory(int userId) {
        List<HarvestLog> result = new ArrayList<>();
        try {
            JSONObject query = new JSONObject();
            query.put("userId", userId);

            JSONObject options = new JSONObject();
            JSONObject sort = new JSONObject();
            sort.put("harvestedAt", -1);
            options.put("sort", sort);

            String params = EMASClient.buildDbParamsWithOptions("find", COLLECTION,
                    null, query.toString(), options.toString());
            String response = client.callFunction(FUNCTION_NAME, params);

            if (response != null) {
                JSONArray arr = parseResultArray(response);
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        HarvestLog log = parseHarvestLogJson(arr.getJSONObject(i));
                        if (log != null) {
                            result.add(log);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public LiveData<Integer> getTotalHarvestCountLive(int userId) {
        executor.execute(() -> {
            int count = getTotalHarvestCount(userId);
            totalHarvestCountLive.postValue(count);
        });
        return totalHarvestCountLive;
    }

    @Override
    public HarvestLog getByRequestId(String requestId) {
        try {
            JSONObject query = new JSONObject();
            query.put("requestId", requestId);

            String params = EMASClient.buildDbParams("findOne", COLLECTION,
                    null, query.toString());
            String response = client.callFunction(FUNCTION_NAME, params);

            if (response != null) {
                JSONObject root = new JSONObject(response);
                JSONObject doc = root.optJSONObject("result");
                if (doc != null) {
                    return parseHarvestLogJson(doc);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void insertHarvestLog(HarvestLog log) {
        try {
            JSONObject doc = new JSONObject();
            doc.put("userId", log.getUserId());
            doc.put("tomatoId", log.getTomatoId());
            doc.put("harvestedAt", log.getHarvestedAt());
            doc.put("requestId", log.getRequestId() != null ? log.getRequestId() : "");

            String params = EMASClient.buildDbParams("insertOne", COLLECTION,
                    doc.toString(), null);
            client.callFunction(FUNCTION_NAME, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getPeriodHarvestCount(int userId, long startTime, long endTime) {
        try {
            JSONObject query = new JSONObject();
            query.put("userId", userId);
            JSONObject timeQuery = new JSONObject();
            timeQuery.put("$gte", startTime);
            timeQuery.put("$lte", endTime);
            query.put("harvestedAt", timeQuery);

            String params = EMASClient.buildDbParams("count", COLLECTION,
                    null, query.toString());
            String response = client.callFunction(FUNCTION_NAME, params);

            if (response != null) {
                JSONObject root = new JSONObject(response);
                JSONObject resObj = root.optJSONObject("result");
                if (resObj != null) return resObj.optInt("total", 0);
                return root.optInt("total", 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
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

    private HarvestLog parseHarvestLogJson(JSONObject json) {
        try {
            HarvestLog log = new HarvestLog();
            log.setLogId(json.optInt("logId", json.optInt("_id", 0)));
            log.setUserId(json.optInt("userId", 0));
            log.setTomatoId(json.optInt("tomatoId", 0));
            log.setHarvestedAt(json.optLong("harvestedAt", 0));
            log.setRequestId(json.optString("requestId", ""));
            return log;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
