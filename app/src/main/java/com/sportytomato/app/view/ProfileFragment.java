package com.sportytomato.app.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.sportytomato.app.R;
import com.sportytomato.app.databinding.FragmentProfileBinding;
import com.sportytomato.app.model.entity.User;
import com.sportytomato.app.repository.EnergyRepository;
import com.sportytomato.app.repository.UserRepository;
import com.sportytomato.app.service.AchievementService;
import com.sportytomato.app.service.CloudSyncService;
import com.sportytomato.app.service.DataExportService;
import com.sportytomato.app.service.EMASClient;
import com.sportytomato.app.service.RepositoryManager;
import com.sportytomato.app.service.StepSensorService;
import com.sportytomato.app.model.database.AppDatabase;
import com.sportytomato.app.util.DialogHelper;
import com.sportytomato.app.util.NotificationHelper;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

public class ProfileFragment extends Fragment {

    private static final String PREF_DEBUG = "debug_prefs";
    private static final String KEY_DEBUG_ENABLED = "debug_manual_steps_enabled";

    private FragmentProfileBinding binding;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    saveAvatarToLocal(uri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RepositoryManager rm = RepositoryManager.getInstance();
        UserRepository userRepo = rm.getUserRepo();
        EnergyRepository energyRepo = rm.getEnergyRepo();

        binding.itemEnergyDetail.setOnClickListener(v -> {
            new Thread(() -> {
                User user = userRepo.getCurrentUser();
                if (user != null) {
                    // 强制同步传感器中尚未写入DB的步数
                    StepSensorService.getInstance(requireContext()).syncPendingSteps();

                    int userId = user.getUserId();
                    int dbSteps = energyRepo.getTodaySteps(userId);
                    int energy = energyRepo.getTodayEnergy(userId);
                    int stepEnergy = energyRepo.getTodayStepEnergy(userId);

                    long sensorSteps = StepSensorService.getInstance(requireContext())
                            .getTodayHardwareSteps();
                    long hcSteps = StepSensorService.getInstance(requireContext())
                            .getHealthConnectSteps();

                    final String msg = "传感器步数: " + sensorSteps
                            + "\nHealth Connect: " + (hcSteps > 0 ? hcSteps + "" : "未获取")
                            + "\n已同步步数: " + dbSteps
                            + "\n步数转换能量: " + stepEnergy
                            + "\n今日净能量: " + energy;
                    new Handler(Looper.getMainLooper()).post(() -> {
                        DialogHelper.showStyledDialog(requireContext(),
                                "能量明细", msg, "确定", null);
                    });
                }
            }).start();
        });

        binding.itemAchievements.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), BadgeWallActivity.class);
            startActivity(intent);
        });

        binding.itemFriendRank.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), RankActivity.class);
            startActivity(intent);
        });

        binding.itemBadgeWall.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), BadgeWallActivity.class);
            startActivity(intent);
        });

        binding.itemNotification.setOnClickListener(v -> toggleNotificationSetting());

        // 夜间模式切换
        int nightMode = requireContext().getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        binding.switchDarkMode.setChecked(nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES);
        binding.switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                    isChecked ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                            : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        });

        binding.itemDataMigration.setOnClickListener(v -> {
            new Thread(() -> {
                User user = userRepo.getCurrentUser();
                if (user == null) return;

                if (!EMASClient.getInstance().isInitialized()) {
                    new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(getContext(), "网络不可用，无法同步", Toast.LENGTH_SHORT).show());
                    return;
                }

                final User u = user;
                new Handler(Looper.getMainLooper()).post(() -> showDataMigrationDialog(u));
            }).start();
        });

        // Export data
        binding.itemExportData.setOnClickListener(v -> {
            final Context ctx = getContext();
            new Thread(() -> {
                User currentUser = userRepo.getCurrentUser();
                if (currentUser == null) return;
                AppDatabase db = ((SportyTomatoApplication) requireActivity().getApplication()).getAppDatabase();
                DataExportService exportService = new DataExportService(db);

                try {
                    Uri uri = exportService.saveToFile(ctx, currentUser.getUserId());
                    String filePath = uri.getPath();
                    // 解码 URI 路径中的 %20 等字符
                    try {
                        filePath = java.net.URLDecoder.decode(filePath, "UTF-8");
                    } catch (Exception ignored) {}
                    final String finalPath = filePath;
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(ctx, "导出成功: " + finalPath, Toast.LENGTH_LONG).show();
                    });
                } catch (Exception e) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(ctx, "导出失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }).start();
        });

        binding.itemLogout.setOnClickListener(v -> {
            DialogHelper.showStyledConfirmDialog(getContext(),
                    "退出登录", "确定要退出登录吗？",
                    "确认", (dialog, which) -> {
                        userRepo.logout();
                        Intent intent = new Intent(getActivity(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        if (getActivity() != null) {
                            getActivity().finish();
                        }
                    },
                    "取消", null);
        });

        binding.itemDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog(userRepo));

        binding.ivAvatar.setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
        });

        binding.tvNickname.setOnClickListener(v -> {
            new Thread(() -> {
                User user = userRepo.getCurrentUser();
                if (user != null) {
                    final String phone;
                    if (user.getPhone() != null && user.getPhone().length() >= 11) {
                        phone = user.getPhone().substring(0, 3) + "****" + user.getPhone().substring(7);
                    } else {
                        phone = "未绑定";
                    }
                    final String nickname = user.getNickname();
                    new Handler(Looper.getMainLooper()).post(() -> {
                        DialogHelper.showStyledConfirmDialog(requireContext(),
                                "个人信息",
                                "手机号: \n" + phone + "\n\n昵称: " + nickname,
                                "修改昵称", (d, w) -> showEditNicknameDialog(user),
                                "关闭", null);
                    });
                }
            }).start();
        });

        binding.idContainer.setOnClickListener(v -> {
            new Thread(() -> {
                User user = userRepo.getCurrentUser();
                if (user != null && user.getShortId() != null) {
                    final String sid = user.getShortId();
                    new Handler(Looper.getMainLooper()).post(() -> {
                        android.content.ClipboardManager clipboard =
                                (android.content.ClipboardManager) requireContext()
                                        .getSystemService(Context.CLIPBOARD_SERVICE);
                        android.content.ClipData clip =
                                android.content.ClipData.newPlainText("user_id", sid);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(getContext(), "ID已复制: " + sid, Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        });

        binding.btnSubmitSteps.setOnClickListener(v -> {
            String input = binding.etManualSteps.getText().toString().trim();
            if (TextUtils.isEmpty(input)) {
                Toast.makeText(getContext(), "请输入步数", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                final int steps = Integer.parseInt(input);
                if (steps <= 0) {
                    Toast.makeText(getContext(), "步数必须大于0", Toast.LENGTH_SHORT).show();
                    return;
                }
                new Thread(() -> {
                    User user = userRepo.getCurrentUser();
                    if (user != null) {
                        energyRepo.addStepsSync(user.getUserId(), steps);
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(getContext(), "已添加 " + steps + " 步", Toast.LENGTH_SHORT).show();
                            binding.etManualSteps.setText("");
                        });
                    }
                }).start();
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "请输入有效数字", Toast.LENGTH_SHORT).show();
            }
        });

        checkDebugMode();
        refreshUserInfo();
    }

    private void pushAvatarToCloud(String shortId, Bitmap bitmap) {
        if (!EMASClient.getInstance().isInitialized()) return;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);
            String base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
            baos.close();

            JSONObject setData = new JSONObject();
            setData.put("avatarUrl", base64);
            JSONObject updateData = new JSONObject();
            updateData.put("$set", setData);

            JSONObject query = new JSONObject();
            query.put("shortId", shortId);

            String params = EMASClient.buildDbParams("updateOne", "users",
                    updateData.toString(), query.toString());

            new Thread(() -> {
                try {
                    String result = EMASClient.getInstance().callFunction("sporty-api", params);
                    android.util.Log.i("Avatar", "pushAvatarToCloud for " + shortId
                            + ": " + (result != null ? "sent, len=" + base64.length() : "null response"));
                } catch (Exception e) {
                    android.util.Log.e("Avatar", "pushAvatarToCloud error: " + e.getMessage());
                }
            }).start();
        } catch (Exception e) {
        }
    }

    private void checkDebugMode() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_DEBUG, Context.MODE_PRIVATE);
        boolean debugEnabled = prefs.getBoolean(KEY_DEBUG_ENABLED, false);
        int visibility = debugEnabled ? View.VISIBLE : View.GONE;
        binding.dividerDebug.setVisibility(visibility);
        binding.tvDebugTitle.setVisibility(visibility);
        binding.itemManualSteps.setVisibility(visibility);
    }

    private void showEditNicknameDialog(User user) {
        android.widget.EditText et = new android.widget.EditText(requireContext());
        et.setText(user.getNickname());
        et.setSingleLine(true);
        et.setPadding(32, 24, 32, 24);
        et.setTextColor(getResources().getColor(com.sportytomato.app.R.color.on_surface));
        et.setHintTextColor(getResources().getColor(com.sportytomato.app.R.color.on_surface_variant));

        DialogHelper.showStyledViewDialog(requireContext(),
                "修改昵称", et,
                "确认", (d, w) -> {
                    String newNickname = et.getText().toString().trim();
                    if (newNickname.isEmpty() || newNickname.equals(user.getNickname())) return;

                    com.sportytomato.app.service.SensitiveWordService sws =
                            new com.sportytomato.app.service.SensitiveWordService(requireContext());
                    String validationError = sws.validateNickname(newNickname);
                    if (validationError != null) {
                        Toast.makeText(getContext(), validationError, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    user.setNickname(newNickname);
                    user.setUpdatedAt(System.currentTimeMillis());
                    RepositoryManager.getInstance().getUserRepo().updateNickname(
                            user.getUserId(), newNickname);
                    binding.tvNickname.setText(newNickname);
                    new Thread(() -> {
                        CloudSyncService.getInstance().syncUserToCloud(user);
                    }).start();
                    Toast.makeText(getContext(), "昵称已修改为: " + newNickname, Toast.LENGTH_SHORT).show();
                },
                "取消", null);
    }

    private void showDeleteAccountDialog(UserRepository userRepo) {
        new Thread(() -> {
            User user = userRepo.getCurrentUser();
            if (user == null) return;

            final boolean isOnline = EMASClient.getInstance().isInitialized();
            final String message;
            if (isOnline) {
                message = "注销账号:\n\n"
                        + "所有本地数据将被永久删除\n"
                        + "云端数据将被清除\n"
                        + "好友关系将被移除\n"
                        + "此操作不可撤销\n\n"
                        + "确定要注销账号吗？";
            } else {
                message = "无网络连接！\n\n"
                        + "删除后:\n"
                        + "本地数据将被永久删除\n"
                        + "云端数据无法同步删除（请在有网络时重试）\n"
                        + "此操作不可撤销\n\n"
                        + "是否强制删除？";
            }

            final User u = user;
            new Handler(Looper.getMainLooper()).post(() -> {
                DialogHelper.showStyledConfirmDialog(getContext(),
                        "注销账号", message,
                        "确认删除", (dialog, which) -> {
                            performDeleteAccount(userRepo, u, isOnline);
                        },
                        "取消", null);
            });
        }).start();
    }

    private void performDeleteAccount(UserRepository userRepo, User user, boolean isOnline) {
        int userId = user.getUserId();

        if (isOnline) {
            Toast.makeText(getContext(), "正在清除云端数据...", Toast.LENGTH_SHORT).show();
            CloudSyncService.getInstance().deleteUserFromCloud(user.getShortId(), userId, (cloudSuccess, cloudMsg) -> {
                deleteLocalAndExit(userRepo, userId, cloudMsg);
            });
        } else {
            Toast.makeText(getContext(), "离线模式，仅清除本地数据", Toast.LENGTH_SHORT).show();
            deleteLocalAndExit(userRepo, userId, "云端未同步（离线删除）");
        }
    }

    private void deleteLocalAndExit(UserRepository userRepo, int userId, String cloudMessage) {
        userRepo.deleteAccount(userId);

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "账号已注销。" + cloudMessage, Toast.LENGTH_LONG).show();

                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                getActivity().finish();
            });
        }
    }

    private void saveAvatarToLocal(Uri uri) {
        InputStream inputStream = null;
        FileOutputStream fos = null;
        try {
            inputStream = requireContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) return;

            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            if (bitmap == null) {
                Toast.makeText(getContext(), "无法读取图片", Toast.LENGTH_SHORT).show();
                return;
            }

            int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
            int x = (bitmap.getWidth() - size) / 2;
            int y = (bitmap.getHeight() - size) / 2;
            Bitmap squareBitmap = Bitmap.createBitmap(bitmap, x, y, size, size);

            squareBitmap = resizeBitmap(squareBitmap, 200);

            File avatarDir = new File(requireContext().getFilesDir(), "avatars");
            if (!avatarDir.exists()) avatarDir.mkdirs();
            File avatarFile = new File(avatarDir, "avatar_" + System.currentTimeMillis() + ".jpg");

            fos = new FileOutputStream(avatarFile);
            squareBitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);

            final String avatarPath = avatarFile.getAbsolutePath();
            final Bitmap finalBitmap = squareBitmap;

            UserRepository userRepo = RepositoryManager.getInstance().getUserRepo();
            new Thread(() -> {
                User user = userRepo.getCurrentUser();
                if (user != null) {
                    userRepo.updateAvatarUrl(user.getUserId(), avatarPath);
                    pushAvatarToCloud(user.getShortId(), finalBitmap);
                }
            }).start();

            binding.ivAvatar.setImageBitmap(getCircularBitmap(squareBitmap));
            Toast.makeText(getContext(), "头像已更新", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "头像上传失败", Toast.LENGTH_SHORT).show();
        } finally {
            try { if (inputStream != null) inputStream.close(); } catch (Exception ignored) {}
            try { if (fos != null) fos.close(); } catch (Exception ignored) {}
        }
    }

    private Bitmap resizeBitmap(Bitmap source, int maxSize) {
        int width = source.getWidth();
        int height = source.getHeight();
        if (width <= maxSize && height <= maxSize) {
            return source;
        }
        float ratio = Math.min((float) maxSize / width, (float) maxSize / height);
        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);
        return Bitmap.createScaledBitmap(source, newWidth, newHeight, true);
    }

    private Bitmap getCircularBitmap(Bitmap source) {
        int size = Math.min(source.getWidth(), source.getHeight());
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        BitmapShader shader = new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        paint.setShader(shader);

        float radius = size / 2f;
        canvas.drawCircle(radius, radius, radius, paint);
        return output;
    }

    private void refreshUserInfo() {
        new Thread(() -> {
            RepositoryManager rm = RepositoryManager.getInstance();
            User user = rm.getUserRepo().getCurrentUser();
            if (user == null) return;

            final String nickname = user.getNickname();
            final String shortId = user.getShortId();
            final int todayEnergy = rm.getEnergyRepo().getTodayEnergy(user.getUserId());
            final int harvestCount = rm.getHarvestRepo().getTotalHarvestCount(user.getUserId());
            final int todaySteps = rm.getEnergyRepo().getTodaySteps(user.getUserId());
            final int growthPercent = Math.min(todaySteps / 100, 100);
            final String avatarUrl = user.getAvatarUrl();

            new Handler(Looper.getMainLooper()).post(() -> {
                if (binding == null) return;

                binding.tvNickname.setText(nickname);

                if (shortId != null && !shortId.isEmpty()) {
                    binding.tvGuestLabel.setText("ID: " + shortId);
                    binding.tvGuestLabel.setTextColor(
                            requireContext().getResources().getColor(com.sportytomato.app.R.color.primary));
                } else {
                    binding.tvGuestLabel.setText("游客模式");
                }

                binding.tvTotalTomatoes.setText(harvestCount + " 次");
                binding.tvTodayGrowth.setText(growthPercent + "%");

                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                    File avatarFile = new File(avatarUrl);
                    if (avatarFile.exists()) {
                        Bitmap bitmap = BitmapFactory.decodeFile(avatarUrl);
                        if (bitmap != null) {
                            binding.ivAvatar.setImageBitmap(getCircularBitmap(bitmap));
                        }
                    }
                }
            });
        }).start();
    }

    private void showDataMigrationDialog(User user) {
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_data_migration, null);

        androidx.appcompat.app.AlertDialog dialog = DialogHelper.showStyledViewOnlyDialog(
                getContext(), dialogView);

        dialogView.findViewById(R.id.btn_download_from_cloud).setOnClickListener(v -> {
            Toast.makeText(getContext(), "正在从云端下载好友资料...", Toast.LENGTH_SHORT).show();
            CloudSyncService.getInstance().downloadFriendProfiles();
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.btn_upload_to_cloud).setOnClickListener(v -> {
            Toast.makeText(getContext(), "正在上传数据到云端...", Toast.LENGTH_SHORT).show();
            CloudSyncService.getInstance().syncAllDataToCloud(user.getUserId(),
                    (success, message) -> {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(),
                                        success ? "上传成功" : "上传失败: " + message,
                                        Toast.LENGTH_LONG).show();
                            });
                        }
                    });
            dialog.dismiss();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding == null) return;
        checkDebugMode();
        refreshUserInfo();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void toggleNotificationSetting() {
        SharedPreferences prefs = requireContext().getSharedPreferences(
                "app_settings", Context.MODE_PRIVATE);
        boolean current = prefs.getBoolean("notification_enabled", true);

        DialogHelper.showStyledConfirmDialog(requireContext(),
                "通知设置", "当前通知: " + (current ? "已开启" : "已关闭"),
                current ? "关闭" : "开启", (d, w) -> {
                    prefs.edit().putBoolean("notification_enabled", !current).apply();
                    if (!current) {
                        NotificationHelper.scheduleEnergyReminder(requireContext());
                    } else {
                        NotificationHelper.cancelEnergyReminder(requireContext());
                    }
                    Toast.makeText(getContext(),
                            "通知已" + (!current ? "开启" : "关闭"), Toast.LENGTH_SHORT).show();
                },
                "取消", null);
    }

    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }
}
