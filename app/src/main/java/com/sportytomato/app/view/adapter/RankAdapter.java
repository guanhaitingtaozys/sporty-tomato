package com.sportytomato.app.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sportytomato.app.databinding.ItemRankBinding;
import com.sportytomato.app.model.entity.User;
import com.sportytomato.app.service.RankService;

import java.util.List;

public class RankAdapter extends RecyclerView.Adapter<RankAdapter.RankViewHolder> {

    private List<RankService.RankEntry> entries;
    private int currentUserId;

    public void setData(List<RankService.RankEntry> entries, int currentUserId) {
        this.entries = entries;
        this.currentUserId = currentUserId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RankViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRankBinding binding = ItemRankBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new RankViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RankViewHolder holder, int position) {
        RankService.RankEntry entry = entries.get(position);
        User user = entry.user;

        holder.binding.tvRank.setText(String.valueOf(entry.rank));
        holder.binding.tvNickname.setText(user.getNickname() != null ? user.getNickname() : "未知");
        holder.binding.tvShortId.setText("ID:" + (user.getShortId() != null ? user.getShortId() : "-"));
        holder.binding.tvHarvestCount.setText(entry.harvestCount + "次");

        boolean isSelf = user.getUserId() == currentUserId;
        holder.binding.tvSelfTag.setVisibility(isSelf ? View.VISIBLE : View.GONE);

        // Highlight self rank
        if (isSelf) {
            holder.binding.tvRank.setTextColor(holder.itemView.getContext().getResources().getColor(
                    com.sportytomato.app.R.color.primary));
        } else {
            holder.binding.tvRank.setTextColor(holder.itemView.getContext().getResources().getColor(
                    com.sportytomato.app.R.color.on_surface));
        }
    }

    @Override
    public int getItemCount() {
        return entries != null ? entries.size() : 0;
    }

    static class RankViewHolder extends RecyclerView.ViewHolder {
        final ItemRankBinding binding;

        RankViewHolder(ItemRankBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
