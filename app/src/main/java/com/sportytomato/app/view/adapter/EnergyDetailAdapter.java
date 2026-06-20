package com.sportytomato.app.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.sportytomato.app.R;
import com.sportytomato.app.model.entity.EnergyRecord;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 能量明细列表适配器，显示能量记录明细
 */
public class EnergyDetailAdapter extends RecyclerView.Adapter<EnergyDetailAdapter.ViewHolder> {

    private List<EnergyRecord> records = new ArrayList<>();

    public void setRecords(List<EnergyRecord> records) {
        this.records = records != null ? records : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_energy_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EnergyRecord record = records.get(position);
        holder.bind(record);
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    /**
     * ViewHolder：显示能量变化量、来源描述、时间
     */
    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvEnergyAmount;
        private final TextView tvSourceDesc;
        private final TextView tvTimeLabel;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEnergyAmount = itemView.findViewById(R.id.tv_energy_amount);
            tvSourceDesc = itemView.findViewById(R.id.tv_source_desc);
            tvTimeLabel = itemView.findViewById(R.id.tv_time_label);
        }

        void bind(EnergyRecord record) {
            // 来源描述
            String sourceLabel;
            String prefix;
            int colorRes;
            int sourceType = record.getSourceType();
            if (sourceType == 0) {
                sourceLabel = "步数转换";
                prefix = "+";
                colorRes = R.color.gain_green;
            } else if (sourceType == 1) {
                sourceLabel = "浇水消耗";
                prefix = "-";
                colorRes = R.color.spend_red;
            } else if (sourceType == 3) {
                sourceLabel = "帮好友浇水";
                prefix = "-";
                colorRes = R.color.spend_red;
            } else {
                sourceLabel = "其他";
                prefix = "";
                colorRes = R.color.spend_red;
            }
            tvEnergyAmount.setText(prefix + record.getEnergyValue());
            tvEnergyAmount.setTextColor(
                    ContextCompat.getColor(itemView.getContext(), colorRes));

            tvSourceDesc.setText(sourceLabel);

            // 格式化时间
            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
            if (record.getCreatedAt() > 0) {
                String timeStr = sdf.format(new Date(record.getCreatedAt()));
                tvTimeLabel.setText(timeStr);
            } else {
                tvTimeLabel.setText("--");
            }
        }
    }
}
