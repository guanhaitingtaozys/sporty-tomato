package com.sportytomato.app.service;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 分享服务：生成分享内容和海报
 */
public class ShareService {

    private static final int POSTER_WIDTH = 1080;
    private static final int POSTER_HEIGHT = 1080;

    // 记录上次使用的文案索引，避免连续重复（线程安全）
    private static final AtomicInteger lastUsedTextIndex = new AtomicInteger(-1);

    // 配色方案
    private static final int COLOR_BG_TOP = Color.parseColor("#4CAF50");
    private static final int COLOR_BG_BOTTOM = Color.parseColor("#81C784");
    private static final int COLOR_CARD = Color.parseColor("#FFFFFF");
    private static final int COLOR_TEXT_PRIMARY = Color.parseColor("#212121");
    private static final int COLOR_TEXT_SECONDARY = Color.parseColor("#757575");
    private static final int COLOR_ACCENT = Color.parseColor("#FF9800");
    private static final int COLOR_TOMATO_RED = Color.parseColor("#E53935");

    /**
     * 生成分享文案（防重复：连续两次不重复）
     *
     * @param harvestCount 累计收获次数
     * @param tomatoStage  当前番茄阶段名称
     * @return 分享文案
     */
    public static String generateShareText(int harvestCount, String tomatoStage) {
        // 备选文案模板
        String[] templates = new String[]{
                "我在「运动番茄」收获了" + harvestCount + "颗番茄（" + tomatoStage + "）！"
                        + "运动让生活更美好，一起来种番茄吧！",

                "又收获啦！在「运动番茄」的" + harvestCount + "颗番茄（" + tomatoStage + "）记录了我的每一步努力~",

                harvestCount + "颗番茄（" + tomatoStage + "）get！运动番茄，让健康看得见！",

                "我的" + tomatoStage + "番茄收获啦！累计" + harvestCount + "颗，欢迎来运动番茄一起种番茄~"
        };

        if (harvestCount == 1) {
            return "我在「运动番茄」收获了第1颗番茄（" + tomatoStage + "）！"
                    + "每天走路就能给番茄浇水，快来一起种番茄吧！";
        }

        // 避开上次使用的索引，防止连续重复（AtomicInteger 保证线程安全）
        int prevIndex = lastUsedTextIndex.get();
        int index;
        do {
            index = (int) (Math.random() * templates.length);
        } while (templates.length > 1 && index == prevIndex);

        lastUsedTextIndex.set(index);
        return templates[index];
    }

    /**
     * 生成分享海报（1080x1080）
     *
     * @param context  上下文
     * @param text     自定义文案
     * @param progress 番茄进度 0-100
     * @return Bitmap 海报图片
     */
    public Bitmap generateSharePoster(Context context, String text, int progress) {
        Bitmap bitmap = Bitmap.createBitmap(POSTER_WIDTH, POSTER_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // ---- 背景渐变 ----
        Paint bgPaint = new Paint();
        bgPaint.setShader(new android.graphics.LinearGradient(
                0, 0, 0, POSTER_HEIGHT,
                COLOR_BG_TOP, COLOR_BG_BOTTOM,
                android.graphics.Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, POSTER_WIDTH, POSTER_HEIGHT, bgPaint);

        // ---- 顶部装饰圆形 ----
        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(Color.argb(40, 255, 255, 255));
        canvas.drawCircle(540, 200, 180, circlePaint);
        canvas.drawCircle(140, 600, 100, circlePaint);
        canvas.drawCircle(940, 700, 80, circlePaint);

        // ---- 白色卡片 ----
        Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardPaint.setColor(COLOR_CARD);
        cardPaint.setShadowLayer(8, 0, 4, Color.argb(30, 0, 0, 0));
        RectF cardRect = new RectF(80, 280, 1000, 880);
        canvas.drawRoundRect(cardRect, 24, 24, cardPaint);

        // ---- 番茄图标（Canvas 绘制） ----
        drawTomatoIcon(canvas, 540, 420, 80);

        // ---- 标题文字 ----
        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(COLOR_TOMATO_RED);
        titlePaint.setTextSize(48);
        titlePaint.setFakeBoldText(true);
        titlePaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("我的番茄收获啦！", 540, 530, titlePaint);

        // ---- 进度条 ----
        int barLeft = 200;
        int barTop = 560;
        int barWidth = 680;
        int barHeight = 36;

        Paint barBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        barBg.setColor(Color.parseColor("#E0E0E0"));
        RectF barBgRect = new RectF(barLeft, barTop, barLeft + barWidth, barTop + barHeight);
        canvas.drawRoundRect(barBgRect, barHeight / 2f, barHeight / 2f, barBg);

        Paint barFg = new Paint(Paint.ANTI_ALIAS_FLAG);
        barFg.setColor(COLOR_BG_TOP);
        int progressWidth = Math.max(barHeight, (int) (barWidth * progress / 100f));
        RectF barFgRect = new RectF(barLeft, barTop, barLeft + progressWidth, barTop + barHeight);
        canvas.drawRoundRect(barFgRect, barHeight / 2f, barHeight / 2f, barFg);

        Paint progressTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressTextPaint.setColor(Color.WHITE);
        progressTextPaint.setTextSize(24);
        progressTextPaint.setFakeBoldText(true);
        progressTextPaint.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics fm = progressTextPaint.getFontMetrics();
        float textY = barTop + barHeight / 2f - fm.descent + (fm.descent - fm.ascent) / 2f;
        canvas.drawText(progress + "%", 540, textY, progressTextPaint);

        // ---- 自定义文案 ----
        TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(COLOR_TEXT_PRIMARY);
        textPaint.setTextSize(32);
        String displayText = text != null ? text : "运动让生活更美好";
        StaticLayout staticLayout = StaticLayout.Builder.obtain(
                        displayText, 0, displayText.length(), textPaint, 600)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(0, 1.5f)
                .build();

        canvas.save();
        canvas.translate(240, 630);
        staticLayout.draw(canvas);
        canvas.restore();

        // ---- 底部品牌文字 ----
        Paint brandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        brandPaint.setColor(Color.argb(180, 255, 255, 255));
        brandPaint.setTextSize(28);
        brandPaint.setTextAlign(Paint.Align.CENTER);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault());
        canvas.drawText("运动番茄  " + sdf.format(new Date()), 540, 970, brandPaint);

        return bitmap;
    }

    /**
     * 使用 Canvas 绘制番茄图标
     */
    private void drawTomatoIcon(Canvas canvas, float cx, float cy, float radius) {
        // 主体圆形
        Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bodyPaint.setColor(COLOR_TOMATO_RED);
        canvas.drawCircle(cx, cy, radius, bodyPaint);

        // 高光
        Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightPaint.setColor(Color.argb(60, 255, 255, 255));
        canvas.drawCircle(cx - radius * 0.3f, cy - radius * 0.3f, radius * 0.35f, highlightPaint);

        // 叶子
        Paint leafPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        leafPaint.setColor(COLOR_BG_TOP);
        leafPaint.setStrokeCap(Paint.Cap.ROUND);
        leafPaint.setStrokeWidth(radius * 0.18f);

        // 茎
        canvas.drawLine(cx, cy - radius + 4, cx, cy - radius - 20, leafPaint);

        // 左叶
        canvas.save();
        canvas.rotate(-35, cx, cy - radius - 5);
        canvas.drawLine(cx, cy - radius - 5, cx + 18, cy - radius - 5, leafPaint);
        canvas.restore();

        // 右叶
        canvas.save();
        canvas.rotate(35, cx, cy - radius - 5);
        canvas.drawLine(cx, cy - radius - 5, cx - 18, cy - radius - 5, leafPaint);
        canvas.restore();
    }

    /**
     * 保存海报到相册（统一使用 MediaStore，所有 Android 版本均适用）
     *
     * @param context 上下文
     * @param bitmap  海报图片
     * @return 是否保存成功
     */
    public boolean savePosterToGallery(Context context, Bitmap bitmap) {
        try {
            String fileName = "sporty_tomato_share_" + System.currentTimeMillis() + ".png";
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/SportyTomato");
            }

            Uri uri = context.getContentResolver()
                    .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try (OutputStream os = context.getContentResolver().openOutputStream(uri)) {
                    if (os != null) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                        os.flush();
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
