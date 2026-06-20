package com.sportytomato.app.service;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.UUID;

public class IdAllocator {
    private static final String PREFS_NAME = "id_allocator_prefs";
    private static final String KEY_DEVICE_OFFSET = "device_offset";
    private static final String KEY_NEXT_ID = "next_id";

    public static synchronized String allocate(Context context) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int offset = prefs.getInt(KEY_DEVICE_OFFSET, -1);
        if (offset < 0) { offset = generateDeviceOffset(context); prefs.edit().putInt(KEY_DEVICE_OFFSET, offset).apply(); }
        int nextId = prefs.getInt(KEY_NEXT_ID, 0);
        prefs.edit().putInt(KEY_NEXT_ID, nextId + 1).apply();
        int id = Math.abs((offset + nextId) % 1000000);
        return String.format("%06d", id);
    }

    private static int generateDeviceOffset(Context context) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences("device_uuid_prefs", Context.MODE_PRIVATE);
        String deviceUuid = prefs.getString("device_uuid", null);
        if (deviceUuid == null) { deviceUuid = UUID.randomUUID().toString(); prefs.edit().putString("device_uuid", deviceUuid).apply(); }
        return Math.abs(deviceUuid.hashCode()) % 1000000;
    }
}