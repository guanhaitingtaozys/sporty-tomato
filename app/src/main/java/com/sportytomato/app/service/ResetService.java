package com.sportytomato.app.service;

import android.content.Context;
import android.content.SharedPreferences;
import com.sportytomato.app.model.database.AppDatabase;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ResetService {
    private static final String PREFS_NAME = "daily_reset_prefs";
    private static final String KEY_LAST_RESET_DATE = "last_reset_date";
    private final AppDatabase database;
    private final SharedPreferences prefs;
    private final ExecutorService executor;

    public ResetService(AppDatabase database, Context context) { this.database = database; this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE); this.executor = Executors.newSingleThreadExecutor(); }
    public boolean needsReset() { String today = getTodayDate(); String lastReset = prefs.getString(KEY_LAST_RESET_DATE, ""); return !today.equals(lastReset); }
    public void performDailyReset() { if (!needsReset()) return; executor.execute(() -> { prefs.edit().putString(KEY_LAST_RESET_DATE, getTodayDate()).apply(); }); }
    private String getTodayDate() { return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()); }
}