package com.sportytomato.app.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;

public class ShareUtil {
    private static final int CARD_WIDTH = 600;
    private static final int CARD_HEIGHT = 800;

    public static void shareHarvest(Context context, String nickname, int harvestCount) {
        Bitmap card = createShareCard(context, "我的收获成绩", nickname + " 的运动番茄", "累计收获：", String.valueOf(harvestCount), "颗番茄");
        shareImage(context, card, "harvest_share.png");
    }

    public static void shareEnergy(Context context, String nickname, int energy, int steps) {
        Bitmap card = createEnergyShareCard(context, "我的运动成绩", nickname + " 的运动番茄", "今日能量：", String.valueOf(energy), steps + " 步转换");
        shareImage(context, card, "energy_share.png");
    }

    private static Bitmap createShareCard(Context context, String title, String line1, String line2, String bigNum, String unit) {
        Bitmap bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint bgPaint = new Paint(); bgPaint.setColor(Color.parseColor("#4CAF50"));
        canvas.drawRoundRect(new RectF(0, 0, CARD_WIDTH, CARD_HEIGHT), 24, 24, bgPaint);
        Paint innerPaint = new Paint(); innerPaint.setColor(Color.WHITE);
        canvas.drawRoundRect(new RectF(16, 16, CARD_WIDTH - 16, CARD_HEIGHT - 16), 16, 16, innerPaint);
        Paint headerPaint = new Paint(); headerPaint.setColor(Color.parseColor("#4CAF50"));
        canvas.drawRoundRect(new RectF(16, 16, CARD_WIDTH - 16, 120), 16, 16, headerPaint);
        canvas.drawRect(new RectF(16, 100, CARD_WIDTH - 16, 120), headerPaint);
        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG); tp.setColor(Color.WHITE); tp.setTextSize(32); tp.setTypeface(Typeface.DEFAULT_BOLD); tp.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(title, CARD_WIDTH / 2f, 60, tp);
        Paint lp = new Paint(Paint.ANTI_ALIAS_FLAG); lp.setColor(Color.parseColor("#333333")); lp.setTextSize(22); lp.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(line1, CARD_WIDTH / 2f, 180, lp);
        lp.setColor(Color.parseColor("#666666")); lp.setTextSize(20);
        canvas.drawText(line2, CARD_WIDTH / 2f, 270, lp);
        Paint bp = new Paint(Paint.ANTI_ALIAS_FLAG); bp.setColor(Color.parseColor("#4CAF50")); bp.setTextSize(80); bp.setTypeface(Typeface.DEFAULT_BOLD); bp.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(bigNum, CARD_WIDTH / 2f, 380, bp);
        lp.setTextSize(24); lp.setColor(Color.parseColor("#666666"));
        canvas.drawText(unit, CARD_WIDTH / 2f, 430, lp);
        Paint fp = new Paint(Paint.ANTI_ALIAS_FLAG); fp.setColor(Color.parseColor("#999999")); fp.setTextSize(16); fp.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("运动番茄 · 走路就能种番茄", CARD_WIDTH / 2f, 500, fp);
        return bitmap;
    }

    private static Bitmap createEnergyShareCard(Context context, String title, String line1, String line2, String bigNum, String subtitle) {
        Bitmap bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint bgPaint = new Paint(); bgPaint.setColor(Color.parseColor("#2196F3"));
        canvas.drawRoundRect(new RectF(0, 0, CARD_WIDTH, CARD_HEIGHT), 24, 24, bgPaint);
        Paint innerPaint = new Paint(); innerPaint.setColor(Color.WHITE);
        canvas.drawRoundRect(new RectF(16, 16, CARD_WIDTH - 16, CARD_HEIGHT - 16), 16, 16, innerPaint);
        Paint headerPaint = new Paint(); headerPaint.setColor(Color.parseColor("#2196F3"));
        canvas.drawRoundRect(new RectF(16, 16, CARD_WIDTH - 16, 120), 16, 16, headerPaint);
        canvas.drawRect(new RectF(16, 100, CARD_WIDTH - 16, 120), headerPaint);
        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG); tp.setColor(Color.WHITE); tp.setTextSize(32); tp.setTypeface(Typeface.DEFAULT_BOLD); tp.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(title, CARD_WIDTH / 2f, 60, tp);
        Paint lp = new Paint(Paint.ANTI_ALIAS_FLAG); lp.setColor(Color.parseColor("#333333")); lp.setTextSize(22); lp.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(line1, CARD_WIDTH / 2f, 180, lp);
        lp.setColor(Color.parseColor("#666666")); lp.setTextSize(20);
        canvas.drawText(line2, CARD_WIDTH / 2f, 270, lp);
        Paint bp = new Paint(Paint.ANTI_ALIAS_FLAG); bp.setColor(Color.parseColor("#2196F3")); bp.setTextSize(72); bp.setTypeface(Typeface.DEFAULT_BOLD); bp.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(bigNum, CARD_WIDTH / 2f, 370, bp);
        lp.setTextSize(20);
        canvas.drawText(subtitle, CARD_WIDTH / 2f, 410, lp);
        Paint fp = new Paint(Paint.ANTI_ALIAS_FLAG); fp.setColor(Color.parseColor("#999999")); fp.setTextSize(16); fp.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("运动番茄 · 走路就能种番茄", CARD_WIDTH / 2f, 490, fp);
        return bitmap;
    }

    private static void shareImage(Context context, Bitmap bitmap, String filename) {
        try { File cacheDir = new File(context.getCacheDir(), "share"); if (!cacheDir.exists()) cacheDir.mkdirs();
            File file = new File(cacheDir, filename); FileOutputStream fos = new FileOutputStream(file); bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos); fos.close();
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND); intent.setType("image/png"); intent.putExtra(Intent.EXTRA_STREAM, uri); intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(intent, "分享到"));
        } catch (Exception e) {}
    }
}