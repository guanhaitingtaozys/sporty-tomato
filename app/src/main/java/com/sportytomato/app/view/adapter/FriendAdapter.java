package com.sportytomato.app.view.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sportytomato.app.R;
import com.sportytomato.app.databinding.ItemFriendBinding;
import com.sportytomato.app.model.entity.Friendship;
import com.sportytomato.app.model.entity.User;

import java.util.ArrayList;
import java.util.List;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {

    private final List<FriendItem> items = new ArrayList<>();
    private final List<FriendItem> allItems = new ArrayList<>();
    private OnFriendClickListener listener;
    private OnFriendLongClickListener longClickListener;

    public interface OnFriendClickListener {
        void onFriendClick(int friendshipId, int friendUserId);
    }

    public interface OnFriendLongClickListener {
        void onFriendLongClick(int friendshipId);
    }

    public void setOnFriendClickListener(OnFriendClickListener listener) {
        this.listener = listener;
    }

    public void setOnFriendLongClickListener(OnFriendLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setFriendData(List<Friendship> friendships, List<User> users, int currentUserId) {
        items.clear();
        allItems.clear();
        for (Friendship f : friendships) {
            User friendUser = null;
            for (User u : users) {
                if (u.getUserId() == getFriendId(f, currentUserId)) {
                    friendUser = u;
                    break;
                }
            }
            if (friendUser != null) {
                FriendItem item = new FriendItem(f, friendUser);
                items.add(item);
                allItems.add(item);
            }
        }
        notifyDataSetChanged();
    }

    public void filter(String text) {
        String lower = text != null ? text.toLowerCase() : "";
        items.clear();
        for (FriendItem item : allItems) {
            if (lower.isEmpty()
                    || (item.user.getNickname() != null && item.user.getNickname().toLowerCase().contains(lower))
                    || (item.user.getShortId() != null && item.user.getShortId().contains(text))) {
                items.add(item);
            }
        }
        notifyDataSetChanged();
    }

    public void clearFilter() {
        items.clear();
        items.addAll(allItems);
        notifyDataSetChanged();
    }

    private int getFriendId(Friendship f, int currentUserId) {
        return f.getUserId() == currentUserId ? f.getFriendUserId() : f.getUserId();
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFriendBinding binding = ItemFriendBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new FriendViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        FriendItem item = items.get(position);
        User user = item.user;
        Friendship friendship = item.friendship;

        holder.binding.tvNickname.setText(user.getNickname() != null ? user.getNickname() : "未知用户");
        holder.binding.tvShortId.setText("ID:" + (user.getShortId() != null ? user.getShortId() : "-"));

        if (friendship.getRemarkName() != null && !friendship.getRemarkName().isEmpty()) {
            holder.binding.tvRemark.setVisibility(View.VISIBLE);
            holder.binding.tvRemark.setText(friendship.getRemarkName());
        } else {
            holder.binding.tvRemark.setVisibility(View.GONE);
        }

        // 最近活跃时间
        long updatedAt = friendship.getUpdatedAt();
        if (updatedAt > 0) {
            long now = System.currentTimeMillis();
            long diff = now - updatedAt;
            if (diff < 3600_000) {
                holder.binding.tvLastActive.setText("最近活跃：1小时内");
            } else if (diff < 86_400_000) {
                holder.binding.tvLastActive.setText("最近活跃：" + (diff / 3600_000) + "小时前");
            } else {
                holder.binding.tvLastActive.setText("最近活跃：" + (diff / 86_400_000) + "天前");
            }
        } else {
            holder.binding.tvLastActive.setText("");
        }

        // 在线状态指示
        holder.binding.dotOnlineStatus.setBackgroundResource(
                updatedAt > 0 && (System.currentTimeMillis() - updatedAt) < 3600_000
                        ? com.sportytomato.app.R.drawable.dot_online
                        : com.sportytomato.app.R.drawable.dot_offline);

        // 加载好友头像
        String avatarUrl = user.getAvatarUrl();
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            try {
                Bitmap bmp = BitmapFactory.decodeFile(avatarUrl);
                if (bmp != null) {
                    holder.binding.ivAvatar.setImageBitmap(bmp);
                } else {
                    holder.binding.ivAvatar.setImageResource(com.sportytomato.app.R.drawable.avatar_placeholder_small);
                }
            } catch (Exception ignored) {
                holder.binding.ivAvatar.setImageResource(com.sportytomato.app.R.drawable.avatar_placeholder_small);
            }
        } else {
            holder.binding.ivAvatar.setImageResource(com.sportytomato.app.R.drawable.avatar_placeholder_small);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFriendClick(friendship.getId(), user.getUserId());
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onFriendLongClick(friendship.getId());
                return true;
            }
            return false;
        });

        // "发消息"按钮 → 跳转聊天页
        holder.binding.btnMessage.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(
                    v.getContext(),
                    com.sportytomato.app.view.ChatActivity.class);
            intent.putExtra("friendship_id", friendship.getId());
            intent.putExtra("friend_user_id", user.getUserId());
            intent.putExtra("friend_nickname", user.getNickname());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class FriendViewHolder extends RecyclerView.ViewHolder {
        final ItemFriendBinding binding;

        FriendViewHolder(ItemFriendBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static class FriendItem {
        final Friendship friendship;
        final User user;

        FriendItem(Friendship friendship, User user) {
            this.friendship = friendship;
            this.user = user;
        }
    }
}
