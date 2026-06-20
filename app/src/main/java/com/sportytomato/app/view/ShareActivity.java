package com.sportytomato.app.view;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.sportytomato.app.R;
import com.sportytomato.app.databinding.ActivityShareBinding;
import com.sportytomato.app.service.ShareService;
import com.sportytomato.app.service.TomatoService;

/**
 * 分享页面：展示生成的海报图片，提供保存、分享、复制文案功能
 */
public class ShareActivity extends AppCompatActivity {

    private ActivityShareBinding binding;
    private Bitmap posterBitmap;
    private String shareText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityShareBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        int harvestCount = getIntent().getIntExtra("harvest_count", 0);
        String tomatoStage = getIntent().getStringExtra("tomato_stage");
        if (tomatoStage == null) {
            tomatoStage = TomatoService.getStageName(0);
        }

        setupToolbar();
        generatePoster(harvestCount, tomatoStage);
        setupButtons(harvestCount, tomatoStage);
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void generatePoster(int harvestCount, String tomatoStage) {
        shareText = ShareService.generateShareText(harvestCount, tomatoStage);

        int progress = getIntent().getIntExtra("progress", 100);
        ShareService shareService = new ShareService();
        posterBitmap = shareService.generateSharePoster(this, shareText, progress);
        binding.ivPoster.setImageBitmap(posterBitmap);
    }

    private void setupButtons(int harvestCount, String tomatoStage) {
        // 保存到相册
        binding.btnSaveToGallery.setOnClickListener(v -> {
            if (posterBitmap != null) {
                ShareService shareService = new ShareService();
                boolean saved = shareService.savePosterToGallery(this, posterBitmap);
                if (saved) {
                    Toast.makeText(this, "已保存到相册", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "保存失败，请检查存储权限", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 分享到微信（使用标准 Intent，预留微信接口）
        binding.btnShareToWechat.setOnClickListener(v -> {
            if (posterBitmap != null) {
                shareToWechat(posterBitmap, shareText);
            }
        });

        // 复制文案
        binding.btnCopyText.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("share_text", shareText);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "文案已复制到剪贴板", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * 分享到微信（使用标准 Intent.ACTION_SEND，预留微信 SDK 接口）
     */
    private void shareToWechat(Bitmap bitmap, String text) {
        try {
            // 先保存图片
            ShareService shareService = new ShareService();
            shareService.savePosterToGallery(this, bitmap);

            // 使用系统分享
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_TEXT, text);

            // 尝试通过微信包名分享
            shareIntent.setPackage("com.tencent.mm");
            try {
                startActivity(Intent.createChooser(shareIntent, "分享到微信"));
            } catch (Exception e) {
                // 微信未安装，回退到通用分享
                shareIntent.setPackage(null);
                startActivity(Intent.createChooser(shareIntent, "分享"));
            }
            // 重置 package
        } catch (Exception e) {
            // 通用分享回退
            Intent fallbackIntent = new Intent(Intent.ACTION_SEND);
            fallbackIntent.setType("text/plain");
            fallbackIntent.putExtra(Intent.EXTRA_TEXT, text);
            startActivity(Intent.createChooser(fallbackIntent, "分享"));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (posterBitmap != null && !posterBitmap.isRecycled()) {
            posterBitmap.recycle();
            posterBitmap = null;
        }
        binding = null;
    }
}
