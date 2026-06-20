package com.sportytomato.app.view;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sportytomato.app.model.entity.Message;
import com.sportytomato.app.model.entity.User;
import com.sportytomato.app.repository.MessageRepository;
import com.sportytomato.app.repository.UserRepository;
import com.sportytomato.app.service.RepositoryManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 聊天页面 — 与指定好友的实时对话
 */
public class ChatActivity extends AppCompatActivity {

    private static final int PAGE_SIZE = 50;

    private RecyclerView rvMessages;
    private EditText etMessage;
    private TextView tvChatTitle;
    private MessageAdapter adapter;
    private MessageRepository messageRepo;
    private UserRepository userRepo;
    private User currentUser;
    private User friendUser;
    private int friendUserId;
    private String friendShortId;

    private boolean isLoadingMore = false;
    private boolean hasMoreMessages = true;
    private int loadedCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.sportytomato.app.R.layout.activity_chat);

        friendUserId = getIntent().getIntExtra("friend_user_id", -1);
        if (friendUserId == -1) {
            finish();
            return;
        }

        RepositoryManager rm = RepositoryManager.getInstance();
        messageRepo = rm.getMessageRepo();
        userRepo = rm.getUserRepo();

        // 后台加载用户数据
        final int fid = friendUserId;
        new Thread(() -> {
            currentUser = userRepo.getCurrentUser();
            friendUser = userRepo.getUserById(fid);

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (currentUser == null || friendUser == null) {
                    Toast.makeText(ChatActivity.this, "用户数据异常", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                friendShortId = friendUser.getShortId();

                tvChatTitle = findViewById(com.sportytomato.app.R.id.tv_chat_title);
                tvChatTitle.setText(friendUser.getNickname());

                findViewById(com.sportytomato.app.R.id.btn_back).setOnClickListener(v -> finish());

                rvMessages = findViewById(com.sportytomato.app.R.id.rv_messages);
                LinearLayoutManager layoutManager = new LinearLayoutManager(ChatActivity.this);
                layoutManager.setStackFromEnd(true);
                rvMessages.setLayoutManager(layoutManager);
                adapter = new MessageAdapter();
                rvMessages.setAdapter(adapter);

                rvMessages.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);
                        if (!isLoadingMore && hasMoreMessages && !recyclerView.canScrollVertically(-1)) {
                            loadMoreMessages();
                        }
                    }
                });

                etMessage = findViewById(com.sportytomato.app.R.id.et_message);
                findViewById(com.sportytomato.app.R.id.btn_send).setOnClickListener(v -> {
                    String text = etMessage.getText().toString().trim();
                    if (text.isEmpty()) return;
                    sendMessage(text);
                    etMessage.setText("");
                });

                loadMessages();
                syncFromCloud();
            });
        }).start();
    }

    private void loadMessages() {
        if (isFinishing() || isDestroyed()) return;
        loadedCount = 0;
        hasMoreMessages = true;

        new Thread(() -> {
            List<Message> msgs = messageRepo.getConversationPaged(
                    currentUser.getUserId(), friendUserId, PAGE_SIZE, 0);
            final List<Message> finalMsgs;
            final int finalLoadedCount;
            final boolean finalHasMore;
            if (msgs != null) {
                Collections.reverse(msgs);
                finalLoadedCount = msgs.size();
                finalHasMore = msgs.size() >= PAGE_SIZE;
                finalMsgs = msgs;
            } else {
                finalLoadedCount = 0;
                finalHasMore = false;
                finalMsgs = new ArrayList<>();
            }
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                loadedCount = finalLoadedCount;
                hasMoreMessages = finalHasMore;
                adapter.setMessages(finalMsgs);
                if (!finalMsgs.isEmpty()) {
                    rvMessages.scrollToPosition(finalMsgs.size() - 1);
                }
            });
        }).start();
    }

    private void loadMoreMessages() {
        if (isLoadingMore || !hasMoreMessages) return;
        isLoadingMore = true;
        Toast.makeText(this, "加载更多...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            List<Message> olderMsgs = messageRepo.getConversationPaged(
                    currentUser.getUserId(), friendUserId, PAGE_SIZE, loadedCount);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (olderMsgs != null && !olderMsgs.isEmpty()) {
                    Collections.reverse(olderMsgs);
                    adapter.prependMessages(olderMsgs);
                    loadedCount += olderMsgs.size();
                    hasMoreMessages = olderMsgs.size() >= PAGE_SIZE;
                } else {
                    hasMoreMessages = false;
                }
                isLoadingMore = false;
            });
        }).start();
    }

    private void syncFromCloud() {
        if (currentUser.getShortId() != null && friendShortId != null) {
            messageRepo.syncMessagesFromCloud(
                    currentUser.getUserId(), currentUser.getShortId(),
                    friendUserId, friendShortId);
            // 延迟刷新，等云端同步完成后再标记已读
            rvMessages.postDelayed(() -> {
                loadMessages();
                // 云端消息落库后再标记已读（后台线程）
                if (!isFinishing() && !isDestroyed()) {
                    new Thread(() -> messageRepo.markRead(friendUserId, currentUser.getUserId())).start();
                }
            }, 1200);
        }
    }

    private void sendMessage(String text) {
        Message msg = Message.createNormal(currentUser.getUserId(), friendUserId, text);
        // 先显示消息（乐观更新）
        adapter.addMessage(msg);
        rvMessages.scrollToPosition(adapter.getItemCount() - 1);
        // 后台写入 DB
        new Thread(() -> messageRepo.sendMessage(msg)).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 每次回到页面刷新最新消息
        if (currentUser != null) {
            loadMessages();
        }
    }

    /**
     * 消息列表适配器 — 仿微信/QQ风格
     */
    private class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

        private final List<Message> messages = new ArrayList<>();
        private final String myAvatarUrl;
        private final String friendAvatarUrl;
        private final String friendNickname;

        MessageAdapter() {
            myAvatarUrl = currentUser.getAvatarUrl();
            friendAvatarUrl = friendUser.getAvatarUrl();
            friendNickname = friendUser.getNickname();
        }

        void setMessages(List<Message> msgs) {
            messages.clear();
            if (msgs != null) messages.addAll(msgs);
            notifyDataSetChanged();
        }

        void addMessage(Message msg) {
            messages.add(msg);
            notifyItemInserted(messages.size() - 1);
        }

        /** 在列表头部插入更早的消息 */
        void prependMessages(List<Message> msgs) {
            if (msgs != null && !msgs.isEmpty()) {
                messages.addAll(0, msgs);
                notifyItemRangeInserted(0, msgs.size());
            }
        }

        @Override
        public int getItemViewType(int position) {
            return messages.get(position).getType() > 0 ? 1 : 0;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(
                    com.sportytomato.app.R.layout.item_message, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Message msg = messages.get(position);
            boolean isMine = msg.getFromUserId() == currentUser.getUserId();

            if (msg.getType() > 0) {
                // 系统消息：居中灰色小字
                holder.llNormal.setVisibility(View.GONE);
                holder.tvSystem.setVisibility(View.VISIBLE);
                holder.tvSystem.setText(msg.getContent());
                return;
            }

            // 普通消息
            holder.tvSystem.setVisibility(View.GONE);
            holder.llNormal.setVisibility(View.VISIBLE);

            // 头像
            if (isMine) {
                holder.frameAvatarLeft.setVisibility(View.GONE);
                holder.frameAvatarRight.setVisibility(View.VISIBLE);
                loadAvatar(holder.ivAvatarRight, myAvatarUrl);
            } else {
                holder.frameAvatarLeft.setVisibility(View.VISIBLE);
                holder.frameAvatarRight.setVisibility(View.GONE);
                loadAvatar(holder.ivAvatarLeft, friendAvatarUrl);
            }

            // 昵称
            if (isMine) {
                holder.tvSenderName.setVisibility(View.GONE);
            } else {
                holder.tvSenderName.setVisibility(View.VISIBLE);
                holder.tvSenderName.setText(friendNickname);
            }

            // 气泡背景和位置
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams)
                    holder.tvMessage.getLayoutParams();
            if (isMine) {
                holder.tvMessage.setBackgroundResource(com.sportytomato.app.R.drawable.badge_lv_bg);
                // 左对齐改成右对齐：整个内容区右对齐
                holder.llContent.setGravity(android.view.Gravity.END);
            } else {
                holder.tvMessage.setBackgroundResource(com.sportytomato.app.R.drawable.card_friend_bg);
                holder.llContent.setGravity(android.view.Gravity.START);
            }

            holder.tvMessage.setText(msg.getContent());
            holder.tvTime.setText(DateFormat.format("HH:mm", msg.getTimestamp()));
        }

        private void loadAvatar(ImageView iv, String url) {
            if (url != null && !url.isEmpty()) {
                try {
                    Bitmap bmp = BitmapFactory.decodeFile(url);
                    if (bmp != null) {
                        iv.setImageBitmap(bmp);
                        return;
                    }
                } catch (Exception ignored) {}
            }
            iv.setBackgroundResource(com.sportytomato.app.R.drawable.avatar_placeholder_small);
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            LinearLayout llNormal;
            FrameLayout frameAvatarLeft;
            FrameLayout frameAvatarRight;
            ImageView ivAvatarLeft;
            ImageView ivAvatarRight;
            LinearLayout llContent;
            TextView tvSenderName;
            TextView tvMessage;
            TextView tvTime;
            TextView tvSystem;

            ViewHolder(View itemView) {
                super(itemView);
                llNormal = itemView.findViewById(com.sportytomato.app.R.id.ll_normal);
                frameAvatarLeft = itemView.findViewById(com.sportytomato.app.R.id.frame_avatar_left);
                frameAvatarRight = itemView.findViewById(com.sportytomato.app.R.id.frame_avatar_right);
                ivAvatarLeft = itemView.findViewById(com.sportytomato.app.R.id.iv_avatar_left);
                ivAvatarRight = itemView.findViewById(com.sportytomato.app.R.id.iv_avatar_right);
                llContent = itemView.findViewById(com.sportytomato.app.R.id.ll_content);
                tvSenderName = itemView.findViewById(com.sportytomato.app.R.id.tv_sender_name);
                tvMessage = itemView.findViewById(com.sportytomato.app.R.id.tv_message);
                tvTime = itemView.findViewById(com.sportytomato.app.R.id.tv_time);
                tvSystem = itemView.findViewById(com.sportytomato.app.R.id.tv_system);
            }
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}
