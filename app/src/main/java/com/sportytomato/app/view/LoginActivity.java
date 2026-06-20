package com.sportytomato.app.view;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.sportytomato.app.databinding.ActivityLoginBinding;
import com.sportytomato.app.model.entity.User;
import com.sportytomato.app.repository.UserRepository;
import com.sportytomato.app.service.CloudSyncService;
import com.sportytomato.app.service.RepositoryManager;

import java.util.UUID;

/**
 * 登录页（应用启动入口）
 * 支持手机号+密码登录/注册 + 访客模式。
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private boolean isRegisterMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 如果已有登录用户，直接进入主界面（后台线程检查，避免主线程 Room 操作）
        UserRepository userRepo = RepositoryManager.getInstance().getUserRepo();
        new Thread(() -> {
            User existingUser = userRepo.getCurrentUser();
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (existingUser != null) {
                    goToMainActivity();
                    return;
                }
                binding = ActivityLoginBinding.inflate(getLayoutInflater());
                setContentView(binding.getRoot());
                initLoginViews();
            });
        }).start();
        return;
    }

    private void initLoginViews() {
        UserRepository userRepo = RepositoryManager.getInstance().getUserRepo();

        EditText etPhone = binding.etPhone;
        EditText etPassword = binding.etPassword;
        EditText etNickname = binding.etNickname;
        TextView tvMode = binding.tvMode;
        TextView tvSwitchMode = binding.tvSwitchMode;

        // 登录/注册按钮（后台线程执行，避免主线程网络调用）
        binding.btnSubmit.setOnClickListener(v -> {
            String phone = etPhone.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(phone)) {
                Toast.makeText(this, "请输入手机号", Toast.LENGTH_SHORT).show();
                return;
            }
            if (phone.length() != 11 || !phone.startsWith("1")) {
                Toast.makeText(this, "请输入正确的手机号", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.length() < 6) {
                Toast.makeText(this, "密码至少6位", Toast.LENGTH_SHORT).show();
                return;
            }

            binding.btnSubmit.setEnabled(false);
            binding.btnSubmit.setText(isRegisterMode ? "注册中..." : "登录中...");

            if (isRegisterMode) {
                // 注册
                String nickname = etNickname.getText().toString().trim();
                if (TextUtils.isEmpty(nickname)) {
                    Toast.makeText(this, "请设置昵称", Toast.LENGTH_SHORT).show();
                    binding.btnSubmit.setEnabled(true);
                    binding.btnSubmit.setText("注册");
                    return;
                }
                String finalNickname = nickname;
                new Thread(() -> {
                    User user = userRepo.registerByPhone(phone, password, finalNickname);
                    if (user != null) {
                        User finalUser = user;
                        RepositoryManager.getInstance().getTomatoRepo()
                                .createInitialTomato(finalUser.getUserId());
                        CloudSyncService.getInstance().syncUserToCloud(finalUser);
                        CloudSyncService.getInstance().restoreUserFromCloud(
                                finalUser.getShortId(), finalUser.getUserId(),
                                (success, msg) -> android.util.Log.d("Login", "restore: " + msg));
                    }
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        binding.btnSubmit.setEnabled(true);
                        binding.btnSubmit.setText("注册");
                        if (user == null) {
                            String error = userRepo.getLastError();
                            String msg = (error != null) ? error : "该手机号已注册，请直接登录";
                            Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(LoginActivity.this, "注册成功！", Toast.LENGTH_SHORT).show();
                            goToMainActivity();
                        }
                    });
                }).start();
            } else {
                // 登录
                new Thread(() -> {
                    User user = userRepo.loginByPhone(phone, password);
                    if (user != null) {
                        User finalUser = user;
                        RepositoryManager.getInstance().getTomatoRepo()
                                .createInitialTomato(finalUser.getUserId());
                        CloudSyncService.getInstance().syncUserToCloud(finalUser);
                        CloudSyncService.getInstance().restoreUserFromCloud(
                                finalUser.getShortId(), finalUser.getUserId(),
                                (success, msg) -> android.util.Log.d("Login", "restore: " + msg));
                    }
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        binding.btnSubmit.setEnabled(true);
                        binding.btnSubmit.setText("登录");
                        if (user == null) {
                            String error = userRepo.getLastError();
                            String msg = (error != null) ? error : "手机号未注册或密码错误";
                            Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(LoginActivity.this, "登录成功！", Toast.LENGTH_SHORT).show();
                            goToMainActivity();
                        }
                    });
                }).start();
            }
        });

        // 切换 登录/注册 模式
        tvSwitchMode.setOnClickListener(v -> {
            isRegisterMode = !isRegisterMode;
            if (isRegisterMode) {
                tvMode.setText("账号注册");
                binding.btnSubmit.setText("注册");
                tvSwitchMode.setText("已有账号？立即登录");
                etNickname.setVisibility(View.VISIBLE);
            } else {
                tvMode.setText("账号登录");
                binding.btnSubmit.setText("登录");
                tvSwitchMode.setText("没有账号？立即注册");
                etNickname.setVisibility(View.GONE);
            }
        });

        // 访客模式体验（后台线程执行 Room 操作，避免主线程 DB 操作）
        binding.tvGuestMode.setOnClickListener(v -> {
            binding.tvGuestMode.setEnabled(false);
            new Thread(() -> {
                UserRepository repo = RepositoryManager.getInstance().getUserRepo();
                String localId = UUID.randomUUID().toString();
                String randomSuffix = String.valueOf((int) (Math.random() * 900000 + 100000));
                String nickname = "番茄玩家" + randomSuffix;
                repo.createGuestUser(localId, nickname);

                User guestUser = repo.getCurrentUser();
                if (guestUser != null) {
                    RepositoryManager.getInstance().getTomatoRepo()
                            .createInitialTomato(guestUser.getUserId());
                }
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    binding.tvGuestMode.setEnabled(true);
                    goToMainActivity();
                });
            }).start();
        });
    }

    private void goToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
