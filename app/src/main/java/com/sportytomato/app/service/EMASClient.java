package com.sportytomato.app.service;

import android.util.Log;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EMASClient {
    private static final String TAG = "EMASClient";
    private static EMASClient instance;
    private String spaceId, secret, endpoint;
    private OkHttpClient httpClient;
    private boolean initialized = false;
    private int lastHttpCode = -1;
    private String lastErrorMessage;

    private EMASClient() {}
    public static synchronized EMASClient getInstance() { if (instance == null) instance = new EMASClient(); return instance; }

    public synchronized void init(String spaceId, String secret, String endpoint) {
        if (initialized) return;
        this.spaceId = spaceId; this.secret = secret; this.endpoint = endpoint;
        this.httpClient = new OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).writeTimeout(15, TimeUnit.SECONDS).build();
        this.initialized = true;
    }

    public boolean isInitialized() { return initialized; }
    public int getLastHttpCode() { return lastHttpCode; }
    public String getLastErrorMessage() { return lastErrorMessage; }

    public String callFunction(String functionName, String params) {
        if (!initialized) return null;
        String url = endpoint + "/" + functionName;
        Request request;
        if (params == null || params.isEmpty()) { request = new Request.Builder().url(url).get().build(); }
        else if (params.startsWith("{")) { request = new Request.Builder().url(url).post(RequestBody.create(params, MediaType.parse("application/json"))).build(); }
        else { String body = keyValueToJson(params); request = new Request.Builder().url(url).post(RequestBody.create(body, MediaType.parse("application/json"))).build(); }
        try (Response response = httpClient.newCall(request).execute()) {
            lastHttpCode = response.code();
            if (response.isSuccessful() && response.body() != null) { String raw = response.body().string(); lastErrorMessage = null; return unwrapResponse(raw); }
            else { lastErrorMessage = "HTTP " + response.code() + ": " + response.message(); return null; }
        } catch (IOException e) { lastHttpCode = -1; lastErrorMessage = e.getClass().getSimpleName() + ": " + e.getMessage(); return null; }
    }

    private String keyValueToJson(String kvParams) {
        try { JSONObject json = new JSONObject(); String[] pairs = kvParams.split("&");
            for (String pair : pairs) { String[] kv = pair.split("=", 2); if (kv.length < 2) continue; String key = URLDecoder.decode(kv[0], "UTF-8"); String value = URLDecoder.decode(kv[1], "UTF-8"); if ((key.equals("data") || key.equals("query") || key.equals("options")) && value.startsWith("{")) { try { json.put(key, new JSONObject(value)); } catch (Exception e) { json.put(key, value); } } else { json.put(key, value); } }
            return json.toString();
        } catch (Exception e) { return "{}"; }
    }

    private String unwrapResponse(String raw) { try { JSONObject top = new JSONObject(raw); if (top.has("statusCode") && top.has("body")) return top.getString("body"); return raw; } catch (Exception e) { return raw; } }

    public JSONObject callFunctionJson(String functionName, String jsonParams) { String result = callFunction(functionName, jsonParams); if (result == null) return null; try { return new JSONObject(result); } catch (Exception e) { return null; } }

    public static String buildDbParams(String action, String collection, String data, String query) {
        try { StringBuilder sb = new StringBuilder(); sb.append("action=").append(URLEncoder.encode(action, "UTF-8")); sb.append("&collection=").append(URLEncoder.encode(collection, "UTF-8")); if (data != null) sb.append("&data=").append(URLEncoder.encode(data, "UTF-8")); if (query != null) sb.append("&query=").append(URLEncoder.encode(query, "UTF-8")); return sb.toString(); } catch (Exception e) { return "action=" + action + "&collection=" + collection; }
    }

    public static String buildDbParamsWithOptions(String action, String collection, String data, String query, String options) {
        try { StringBuilder sb = new StringBuilder(); sb.append("action=").append(URLEncoder.encode(action, "UTF-8")); sb.append("&collection=").append(URLEncoder.encode(collection, "UTF-8")); if (data != null) sb.append("&data=").append(URLEncoder.encode(data, "UTF-8")); if (query != null) sb.append("&query=").append(URLEncoder.encode(query, "UTF-8")); if (options != null) sb.append("&options=").append(URLEncoder.encode(options, "UTF-8")); return sb.toString(); } catch (Exception e) { return "action=" + action + "&collection=" + collection; }
    }
}