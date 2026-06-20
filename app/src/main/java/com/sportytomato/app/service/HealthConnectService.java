package com.sportytomato.app.service;

import android.content.Context;
import androidx.health.connect.client.HealthConnectClient;
import androidx.health.connect.client.records.StepsRecord;
import androidx.health.connect.client.request.AggregateRequest;
import androidx.health.connect.client.time.TimeRangeFilter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HealthConnectService {
    private static volatile HealthConnectService instance;
    private HealthConnectClient client;
    private boolean available = false;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private HealthConnectService(Context context) { initClient(context.getApplicationContext()); }
    public static synchronized HealthConnectService getInstance(Context context) { if (instance == null) instance = new HealthConnectService(context); return instance; }
    private void initClient(Context context) { try { client = HealthConnectClient.getOrCreate(context); available = true; } catch (Exception e) { available = false; } }
    public boolean isAvailable() { return available && client != null; }
    public HealthConnectClient getClient() { return client; }

    public void readTodaySteps(StepCallback callback) {
        if (!isAvailable()) { callback.onResult(0); return; }
        executor.execute(() -> {
            try {
                LocalDate today = LocalDate.now();
                Instant startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
                Instant now = Instant.now();
                AggregateRequest request = new AggregateRequest(Collections.singleton(StepsRecord.COUNT_TOTAL), TimeRangeFilter.between(startOfDay, now), Collections.emptySet());
                long steps = HealthConnectBridge.readTodaySteps(client, request);
                callback.onResult(steps);
            } catch (Exception e) { callback.onResult(0); }
        });
    }
    public interface StepCallback { void onResult(long steps); }
    public void shutdown() { executor.shutdown(); }
}