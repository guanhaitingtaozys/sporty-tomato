package com.sportytomato.app.view;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sportytomato.app.R;
import com.sportytomato.app.databinding.FragmentHomeBinding;
import com.sportytomato.app.model.entity.TomatoState;
import com.sportytomato.app.model.entity.User;
import com.sportytomato.app.repository.EnergyRepository;
import com.sportytomato.app.repository.HarvestRepository;
import com.sportytomato.app.repository.TomatoRepository;
import com.sportytomato.app.repository.TomatoRepositoryImpl;
import com.sportytomato.app.repository.UserRepository;
import com.sportytomato.app.service.AchievementService;
import com.sportytomato.app.service.HarvestService;
import com.sportytomato.app.service.DryPenaltyService;
import com.sportytomato.app.service.EMASClient;
import com.sportytomato.app.service.RepositoryManager;
import com.sportytomato.app.service.StepSensorService;
import com.sportytomato.app.service.TomatoService;
import com.sportytomato.app.util.DialogHelper;
import com.sportytomato.app.util.NotificationHelper;
import com.sportytomato.app.util.ShareUtil;

import org.json.JSONObject;

/**
 * 首页 Fragment: 步数/能量/番茄状态/浇水/收获
 * Sprint 2: 使用 RepositoryManager 获取数据源，触发成就检查和通知
 */
public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private AchievementService achievementService;
    private DryPenaltyService dryPenaltyService;
    private TomatoState currentTomato;
    private boolean ripeNotified = false;
    private Handler countdownHandler;
    private Runnable countdownUpdater;
    private User currentUser;

    // 缓存值，供每秒倒计时更新使用（避免每秒查DB）
    private long cachedLastWateredAt = 0;
    private int cachedStepEnergy = 0;
    private int cachedNetEnergy = 0;
    private int cachedConsumed = 0;

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        achievementService = ((SportyTomatoApplication) requireActivity().getApplication())
                .getAchievementService();
        dryPenaltyService = ((SportyTomatoApplication) requireActivity().getApplication())
                .getDryPenaltyService();

        // 后台线程加载用户后设置点击处理
        new Thread(() -> {
            RepositoryManager rm = RepositoryManager.getInstance();
            UserRepository userRepo = rm.getUserRepo();
            currentUser = userRepo.getCurrentUser();
            if (currentUser == null) {
                return;
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                if (binding == null) return;
                setupClickListeners();
                // 启动番茄浮动动画
                startTomatoFloatingAnimation();
                // 启动倒计时实时更新
                startCountdownTimer();
            });
        }).start();
    }

    private void setupClickListeners() {
        if (currentUser == null) return;

        RepositoryManager rm = RepositoryManager.getInstance();
        EnergyRepository energyRepo = rm.getEnergyRepo();
        TomatoRepository tomatoRepo = rm.getTomatoRepo();
        HarvestRepository harvestRepo = rm.getHarvestRepo();

        binding.btnWater.setOnClickListener(v -> {
            // 按钮缩放反馈 + 立即禁用防快速双击
            v.startAnimation(AnimationUtils.loadAnimation(getContext(), com.sportytomato.app.R.anim.btn_press_anim));
            binding.btnWater.setEnabled(false);

            final int userId = currentUser.getUserId();
            new Thread(() -> {
                TomatoState tomato = tomatoRepo.getTomato(userId);
                if (tomato == null) {
                    tomatoRepo.createInitialTomato(userId);
                    tomato = tomatoRepo.getTomato(userId);
                    if (tomato == null) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            binding.btnWater.setEnabled(true);
                            Toast.makeText(getContext(), "番茄未初始化", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }
                }
                TomatoRepository.WaterResult result = tomatoRepo.water(userId, tomato.getTomatoId());
                if (result.success) {
                    int actualWaterings = ((SportyTomatoApplication) requireActivity().getApplication())
                            .getAppDatabase().wateringLogDao().getTotalWateringCount(userId);
                    achievementService.checkAndUnlock(userId, 3, actualWaterings);
                    DryPenaltyService.DryResult dryResult = null;
                    if (dryPenaltyService != null) {
                        dryResult = dryPenaltyService.checkAndApply(userId);
                    }
                    final DryPenaltyService.DryResult finalDryResult = dryResult;
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (binding == null) return;
                        binding.btnWater.setEnabled(true);
                        playWateringAnimation();
                        playTomatoGrowAnimation();
                        Toast.makeText(getContext(), result.message, Toast.LENGTH_SHORT).show();
                        if (finalDryResult != null && finalDryResult.isWarning) {
                            Toast.makeText(getContext(), "番茄即将干涸，请及时浇水！", Toast.LENGTH_SHORT).show();
                        }
                        refreshData();
                    });
                } else {
                    final String msg = result.message;
                    new Handler(Looper.getMainLooper()).post(() -> {
                        binding.btnWater.setEnabled(true);
                        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        });

        binding.btnHarvest.setOnClickListener(v -> {
            // 按钮缩放反馈（纯 UI，主线程安全）
            v.startAnimation(AnimationUtils.loadAnimation(getContext(), com.sportytomato.app.R.anim.btn_press_anim));

            final int userId = currentUser.getUserId();
            final String userNickname = currentUser.getNickname();
            new Thread(() -> {
                TomatoState tomato = tomatoRepo.getTomato(userId);
                if (tomato == null || tomato.getProgress() < 100) {
                    new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(getContext(), "番茄还未成熟", Toast.LENGTH_SHORT).show());
                    return;
                }
                TomatoRepository.HarvestResult result = tomatoRepo.harvest(userId, tomato.getTomatoId());
                if (result.success) {
                    String donateMessage = HarvestService.getRandomDonateMessage();
                    int harvestCount = harvestRepo.getTotalHarvestCount(userId);
                    final int finalCount = harvestCount;
                    achievementService.checkAndUnlock(userId, 0, harvestCount);
                    achievementService.checkAndUnlock(userId, 1, harvestCount);
                    achievementService.checkAndUnlock(userId, 2, harvestCount);

                    final String message = donateMessage + "\n\n累计收获: " + harvestCount + " 次";

                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (binding == null) return;
                        new MaterialAlertDialogBuilder(getContext())
                                .setTitle("收获成功！")
                                .setMessage(message)
                                .setPositiveButton("好的", (dialog, which) -> {
                                    dialog.dismiss();
                                    refreshData();
                                })
                                .setNeutralButton("分享", (dialog, which) ->
                                        ShareUtil.shareHarvest(requireContext(), userNickname, finalCount))
                                .show();
                    });
                } else {
                    final String msg = result.message;
                    new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show());
                }
            }).start();
        });

        // 手动刷新步数按钮
        binding.btnRefreshSteps.setOnClickListener(v -> {
            StepSensorService.getInstance(requireContext()).refreshHealthConnectSteps();
            long sensorSteps = StepSensorService.getInstance(requireContext()).getTodaySensorSteps();
            Toast.makeText(getContext(), "传感器今日步数: " + sensorSteps, Toast.LENGTH_SHORT).show();
            refreshData();
        });

        // 启动倒计时实时更新
        startCountdownTimer();
    }

    /**
     * 启动倒计时每秒更新定时器
     */
    /**
     * 启动倒计时每秒更新定时器（纯客户端计算，不做DB查询）
     */
    private void startCountdownTimer() {
        countdownHandler = new Handler(Looper.getMainLooper());
        countdownUpdater = new Runnable() {
            @Override
            public void run() {
                if (binding == null) {
                    return;
                }
                // 纯客户端倒计时计算
                String countdown = "--:--:--";
                if (cachedLastWateredAt > 0) {
                    long elapsed = System.currentTimeMillis() - cachedLastWateredAt;
                    long remaining = Math.max(0, 24 * 60 * 60 * 1000L - elapsed);
                    long hours = remaining / (60 * 60 * 1000);
                    long minutes = (remaining % (60 * 60 * 1000)) / (60 * 1000);
                    long seconds = (remaining % (60 * 1000)) / 1000;
                    countdown = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                }

                String energyInfo = "步数转换: " + cachedStepEnergy + " | 浇水消耗: -" + cachedConsumed + " | 净能量: " + cachedNetEnergy;
                if (!"--:--:--".equals(countdown)) {
                    binding.tvEnergyHint.setText(energyInfo
                            + " | 浇水倒计时: " + countdown);
                } else {
                    binding.tvEnergyHint.setText(energyInfo);
                }
                // 实时更新能量进度条（使用缓存值）
                int energyPercent = Math.min(cachedStepEnergy * 100 / 2000, 100);
                binding.progressEnergy.setProgress(energyPercent);
                binding.tvEnergyText.setText(cachedStepEnergy + "/2000 (可用: " + cachedNetEnergy + ")");
                countdownHandler.postDelayed(this, 1000);
            }
        };
        countdownHandler.post(countdownUpdater);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshData();
        // 心跳：更新本地好友活跃时间
        updateLocalActiveTime();
        // 触发消息拉取（确保好友Tab小红点在进入App时就生效）
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).triggerMessagePoll();
        }
    }

    private void updateLocalActiveTime() {
        if (currentUser == null) return;
        try {
            // 节流：60秒内不重复执行
            android.content.SharedPreferences sp = requireActivity()
                    .getSharedPreferences("heartbeat_throttle", android.content.Context.MODE_PRIVATE);
            long lastHeartbeat = sp.getLong("last_heartbeat_" + currentUser.getUserId(), 0);
            long now = System.currentTimeMillis();
            if (now - lastHeartbeat < 60_000) return;
            sp.edit().putLong("last_heartbeat_" + currentUser.getUserId(), now).apply();
            new Thread(() -> {
                try {
                    com.sportytomato.app.model.database.AppDatabase db =
                            com.sportytomato.app.model.database.AppDatabase
                                    .getInstance(requireActivity());
                    java.util.List<com.sportytomato.app.model.entity.Friendship> list =
                            db.friendDao().getAcceptedFriendships(currentUser.getUserId());
                    for (com.sportytomato.app.model.entity.Friendship f : list) {
                        f.setUpdatedAt(now);
                        db.friendDao().update(f);
                    }
                    // 云端推送
                    if (EMASClient.getInstance().isInitialized() && currentUser.getShortId() != null) {
                        org.json.JSONObject setData = new org.json.JSONObject();
                        setData.put("updatedAt", now);
                        org.json.JSONObject updateData = new org.json.JSONObject();
                        updateData.put("$set", setData);
                        org.json.JSONObject query = new org.json.JSONObject();
                        query.put("fromShortId", currentUser.getShortId());
                        query.put("status", 1);
                        String params = EMASClient.buildDbParams("updateMany", "friendships",
                                updateData.toString(), query.toString());
                        EMASClient.getInstance().callFunction("sporty-api", params);
                    }
                } catch (Exception ignored) {}
            }).start();
        } catch (Exception ignored) {}
    }

    private void refreshData() {
        if (binding == null || currentUser == null) return;

        final int userId = currentUser.getUserId();
        final long sensorSteps = StepSensorService.getInstance(requireContext()).getTodaySensorSteps();

        new Thread(() -> {
            RepositoryManager rm = RepositoryManager.getInstance();
            EnergyRepository energyRepo = rm.getEnergyRepo();
            TomatoRepository tomatoRepo = rm.getTomatoRepo();

            int todaySteps = energyRepo.getTodaySteps(userId);
            int todayEnergy = energyRepo.getTodayStepEnergy(userId);
            int netEnergy = energyRepo.getTodayEnergy(userId);
            String todayStr = energyRepo.getTodayDate();
            int consumed = Math.abs(energyRepo.getEnergyBySourceType(userId, todayStr, 1));

            TomatoState tomato = tomatoRepo.getTomato(userId);
            boolean penaltyApplied = false;
            String penaltyMessage = null;
            boolean isWarning = false;
            int recheckStage = -1;
            int recheckProgress = -1;

            // 更新缓存值（供每秒倒计时使用）
            cachedStepEnergy = todayEnergy;
            cachedNetEnergy = netEnergy;
            cachedConsumed = consumed;
            if (tomato != null) {
                cachedLastWateredAt = tomato.getLastWateredAt();
            }

            if (tomato != null && dryPenaltyService != null) {
                DryPenaltyService.DryResult dryResult = dryPenaltyService.checkAndApply(userId);
                if (dryResult.penaltyApplied) {
                    penaltyApplied = true;
                    penaltyMessage = dryResult.message;
                    tomato = tomatoRepo.getTomato(userId);
                    if (tomato != null) {
                        recheckStage = tomato.getStage();
                        recheckProgress = tomato.getProgress();
                    }
                } else if (dryResult.isWarning) {
                    isWarning = true;
                }
            }

            final int finalTodayEnergy = todayEnergy;
            final int finalNetEnergy = netEnergy;
            final int finalConsumed = consumed;
            final boolean finalPenaltyApplied = penaltyApplied;
            final String finalPenaltyMessage = penaltyMessage;
            final boolean finalIsWarning = isWarning;
            final int finalRecheckStage = recheckStage;
            final int finalRecheckProgress = recheckProgress;
            final TomatoState finalTomato = tomato;

            new Handler(Looper.getMainLooper()).post(() -> {
                if (binding == null) return;

                binding.tvTodaySteps.setText(String.valueOf(sensorSteps));
                int energyPercent = Math.min(finalTodayEnergy * 100 / 2000, 100);
                binding.progressEnergy.setProgress(energyPercent);
                binding.tvEnergyText.setText(finalTodayEnergy + "/2000 (可用: " + finalNetEnergy + ")");
                binding.tvEnergyHint.setText("步数转换: " + finalTodayEnergy
                        + " | 浇水消耗: -" + finalConsumed + " | 净能量: " + finalNetEnergy
                        + " | 传感器: " + sensorSteps + "步");

                if (finalTomato != null) {
                    binding.tvTomatoStage.setText(TomatoService.getStageName(finalTomato.getStage()));
                    int waterCount = finalTomato.getProgress() / 20;
                    binding.tvProgressPercent.setText(waterCount + "/5 (" + finalTomato.getProgress() + "%)");

                    updateTomatoStageImage(finalTomato.getStage(), finalTomato.getProgress());

                    if (finalTomato.getProgress() >= 100) {
                        binding.btnHarvest.setVisibility(View.VISIBLE);
                        binding.btnWater.setVisibility(View.GONE);
                        binding.btnRefreshSteps.setVisibility(View.GONE);
                        if (!ripeNotified) {
                            NotificationHelper.sendTomatoRipeNotification(requireContext());
                            ripeNotified = true;
                        }
                    } else {
                        binding.btnHarvest.setVisibility(View.GONE);
                        binding.btnWater.setVisibility(View.VISIBLE);
                        binding.btnRefreshSteps.setVisibility(View.VISIBLE);
                        ripeNotified = false;
                    }

                    if (finalPenaltyApplied) {
                        Toast.makeText(getContext(), finalPenaltyMessage, Toast.LENGTH_LONG).show();
                        if (finalRecheckStage >= 0) {
                            binding.tvTomatoStage.setText(TomatoService.getStageName(finalRecheckStage));
                            int wc = finalRecheckProgress / 20;
                            binding.tvProgressPercent.setText(wc + "/5 (" + finalRecheckProgress + "%)");
                            updateTomatoStageImage(finalRecheckStage, finalRecheckProgress);
                        }
                    } else if (finalIsWarning) {
                        String countdown = "--:--:--";
                        if (cachedLastWateredAt > 0) {
                            long remaining = Math.max(0, 24 * 60 * 60 * 1000L - (System.currentTimeMillis() - cachedLastWateredAt));
                            long h = remaining / (60 * 60 * 1000);
                            long m = (remaining % (60 * 60 * 1000)) / (60 * 1000);
                            long s = (remaining % (60 * 1000)) / 1000;
                            countdown = String.format("%02d:%02d:%02d", h, m, s);
                        }
                        Toast.makeText(getContext(), "番茄即将干涸！请在" + countdown + "内浇水", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }).start();
    }

    /**
     * 根据番茄阶段和进度动态切换图片
     * stage: 0=种子, 1=小苗, 2=开花, 3=结果(progress达到100即成熟可收获)
     */
    private void updateTomatoStageImage(int stage, int progress) {
        int resId;
        switch (stage) {
            case 0:
                resId = R.drawable.tomato_stage_seed;
                break;
            case 1:
                resId = R.drawable.tomato_stage_sprout;
                break;
            case 2:
                resId = R.drawable.tomato_stage_bloom;
                break;
            case 3:
                // stage=3，进度达到100即为成熟可收获
                resId = progress >= 100 ? R.drawable.tomato_stage_ripe : R.drawable.tomato_stage_mature;
                break;
            default:
                resId = R.drawable.tomato_stage_seed;
                break;
        }
        binding.viewTomatoImage.setImageResource(resId);
    }

    /**
     * 播放水壶浇水动画
     */
    private void playWateringAnimation() {
        binding.viewWateringOverlay.setVisibility(View.VISIBLE);
        Animation wateringAnim = AnimationUtils.loadAnimation(getContext(), R.anim.watering_anim);
        wateringAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                binding.viewWateringOverlay.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        binding.viewWateringOverlay.startAnimation(wateringAnim);
    }

    /**
     * 播放番茄生长弹跳动画
     */
    private void playTomatoGrowAnimation() {
        Animation growAnim = AnimationUtils.loadAnimation(getContext(), R.anim.tomato_grow_anim);
        binding.viewTomatoImage.startAnimation(growAnim);
    }

    /**
     * 番茄浮动呼吸动画（持续循环）
     */
    private void startTomatoFloatingAnimation() {
        if (getContext() == null) return;
        Animation floating = AnimationUtils.loadAnimation(getContext(), R.anim.tomato_floating);
        binding.viewTomatoImage.startAnimation(floating);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countdownHandler != null && countdownUpdater != null) {
            countdownHandler.removeCallbacks(countdownUpdater);
            countdownHandler = null;
        }
        binding = null;
    }
}
