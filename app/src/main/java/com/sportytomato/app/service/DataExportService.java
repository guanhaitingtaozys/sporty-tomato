package com.sportytomato.app.service;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import com.sportytomato.app.model.database.AppDatabase;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DataExportService {
    private final AppDatabase database;
    public DataExportService(AppDatabase database) { this.database = database; }

    public String exportToCsv(int userId) {
        StringBuilder sb = new StringBuilder(); sb.append("日期,步数,步数能量,浇水消耗,净能量,收获次数,浇水次数\n");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        for (int i = 29; i >= 0; i--) {
            cal.setTime(new Date()); cal.add(Calendar.DAY_OF_YEAR, -i); String date = dateFormat.format(cal.getTime());
            long startOfDay = getStartOfDay(cal); long endOfDay = getEndOfDay(cal);
            int steps = 0, stepEnergy = 0, wateringEnergy = 0, netEnergy = 0, harvestCount = 0, wateringCount = 0;
            try { steps = database.stepRecordDao().getTodayTotalSteps(userId, date); } catch (Exception ignored) {}
            try { stepEnergy = database.energyRecordDao().getTodayStepEnergy(userId, date); } catch (Exception ignored) {}
            try { Integer val = database.energyRecordDao().getEnergyBySourceType(userId, date, 1); if (val != null) wateringEnergy = val; } catch (Exception ignored) {}
            try { netEnergy = database.energyRecordDao().getTodayNetEnergy(userId, date); } catch (Exception ignored) {}
            try { harvestCount = database.harvestLogDao().getPeriodHarvestCount(userId, startOfDay, endOfDay); } catch (Exception ignored) {}
            try { wateringCount = database.wateringLogDao().getPeriodWateringCount(userId, startOfDay, endOfDay); } catch (Exception ignored) {}
            sb.append(date).append(",").append(steps).append(",").append(stepEnergy).append(",").append(wateringEnergy).append(",").append(netEnergy).append(",").append(harvestCount).append(",").append(wateringCount).append("\n");
        }
        return sb.toString();
    }

    public Uri saveToFile(Context context, int userId) throws IOException {
        String csv = exportToCsv(userId);
        String fileName = "sporty_tomato_export_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".csv";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues(); values.put(MediaStore.Downloads.DISPLAY_NAME, fileName); values.put(MediaStore.Downloads.MIME_TYPE, "text/csv"); values.put(MediaStore.Downloads.IS_PENDING, 1);
            Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new IOException("无法创建下载文件");
            try (OutputStream os = context.getContentResolver().openOutputStream(uri)) { if (os == null) throw new IOException("无法打开输出流"); os.write(csv.getBytes("UTF-8")); os.flush(); }
            values.clear(); values.put(MediaStore.Downloads.IS_PENDING, 0); context.getContentResolver().update(uri, values, null, null);
            return uri;
        } else {
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS); if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, fileName);
            try (FileOutputStream fos = new FileOutputStream(file)) { fos.write(csv.getBytes("UTF-8")); fos.flush(); }
            return Uri.fromFile(file);
        }
    }

    private long getStartOfDay(Calendar cal) { Calendar c = (Calendar) cal.clone(); c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0); return c.getTimeInMillis(); }
    private long getEndOfDay(Calendar cal) { Calendar c = (Calendar) cal.clone(); c.set(Calendar.HOUR_OF_DAY, 23); c.set(Calendar.MINUTE, 59); c.set(Calendar.SECOND, 59); c.set(Calendar.MILLISECOND, 999); return c.getTimeInMillis(); }
}