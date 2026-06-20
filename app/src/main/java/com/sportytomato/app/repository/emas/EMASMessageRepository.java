package com.sportytomato.app.repository.emas;

import com.sportytomato.app.model.entity.Message;
import com.sportytomato.app.service.EMASClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * EMAS Serverless 消息云端操作
 */
public class EMASMessageRepository {

    private static final String COLLECTION = "messages";
    private static final String FUNCTION_NAME = "sporty-api";

    /**
     * 推送消息到 EMAS 云端（使用 shortId）
     */
    public void pushToCloud(Message message, String fromShortId, String toShortId) {
        try {
            JSONObject doc = new JSONObject();
            doc.put("fromShortId", fromShortId);
            doc.put("toShortId", toShortId);
            doc.put("content", message.getContent());
            doc.put("type", message.getType());
            doc.put("timestamp", message.getTimestamp());
            doc.put("read", message.getRead());

            String params = EMASClient.buildDbParams("insertOne", COLLECTION,
                    doc.toString(), null);
            EMASClient.getInstance().callFunction(FUNCTION_NAME, params);
        } catch (Exception e) {
            android.util.Log.e("EMASMsgRepo", "推送消息异常: " + e.getMessage());
        }
    }

    /**
     * 从 EMAS 云端拉取两人的会话记录（按时间升序）
     * @return 消息列表，未映射 userId，需要调用方自行映射
     */
    public List<CloudMessage> fetchConversation(String shortIdA, String shortIdB) {
        List<CloudMessage> result = new ArrayList<>();

        try {
            JSONArray orArray = new JSONArray();
            JSONObject cond1 = new JSONObject();
            cond1.put("fromShortId", shortIdA);
            cond1.put("toShortId", shortIdB);
            orArray.put(cond1);
            JSONObject cond2 = new JSONObject();
            cond2.put("fromShortId", shortIdB);
            cond2.put("toShortId", shortIdA);
            orArray.put(cond2);

            JSONObject query = new JSONObject();
            query.put("$or", orArray);

            JSONObject options = new JSONObject();
            JSONObject sort = new JSONObject();
            sort.put("timestamp", 1);
            options.put("sort", sort);

            String params = EMASClient.buildDbParamsWithOptions("find", COLLECTION,
                    null, query.toString(), options.toString());
            String response = EMASClient.getInstance().callFunction(FUNCTION_NAME, params);

            if (response != null) {
                JSONObject root = new JSONObject(response);
                JSONArray arr = root.optJSONArray("result");
                if (arr == null) {
                    JSONObject resObj = root.optJSONObject("result");
                    if (resObj != null && resObj.has("data")) {
                        arr = resObj.optJSONArray("data");
                    }
                }
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        CloudMessage cm = new CloudMessage();
                        cm.fromShortId = obj.optString("fromShortId", "");
                        cm.toShortId = obj.optString("toShortId", "");
                        cm.content = obj.optString("content", "");
                        cm.type = obj.optInt("type", 0);
                        cm.timestamp = obj.optLong("timestamp", 0);
                        cm.read = obj.optInt("read", 0);
                        result.add(cm);
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e("EMASMsgRepo", "拉取会话消息异常: " + e.getMessage());
        }
        return result;
    }

    /**
     * 云端返回的消息原始结构
     */
    public static class CloudMessage {
        public String fromShortId;
        public String toShortId;
        public String content;
        public int type;
        public long timestamp;
        public int read;
    }
}
