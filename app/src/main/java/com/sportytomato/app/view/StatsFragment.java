package com.sportytomato.app.view;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.sportytomato.app.R;
import com.sportytomato.app.databinding.FragmentStatsBinding;
import com.sportytomato.app.model.entity.EnergyRecord;
import com.sportytomato.app.model.entity.User;
import com.sportytomato.app.repository.EnergyRepository;
import com.sportytomato.app.repository.HarvestRepository;
import com.sportytomato.app.repository.StatsRepository;
import com.sportytomato.app.repository.UserRepository;
import com.sportytomato.app.service.RepositoryManager;
import com.sportytomato.app.view.adapter.EnergyDetailAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * 统计看板
 * Sprint 2: 增加能量明细 RecyclerView、周期切换（本周/本月）
 * Sprint 3: 实现真实能量明细数据、MPAndroidChart 图表
 */
public class StatsFragment extends Fragment {

    private FragmentStatsBinding binding;
    private EnergyDetailAdapter adapter;
    private LineChart lineChart;
    private PieChart pieChart;
    private boolean mIsWeek = true;
    private boolean mDataLoaded = false;

    public static StatsFragment newInstance() {
        return new StatsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStatsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 初始化 RecyclerView
        RecyclerView recyclerView = binding.recyclerEnergyDetail;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new EnergyDetailAdapter();
        recyclerView.setAdapter(adapter);

        // 初始化图表
        lineChart = binding.chartEnergyTrend;
        pieChart = binding.chartEnergySource;

        // 本周 / 本月丸形切换
        binding.btnWeek.setOnClickListener(v -> {
            setPillActive(binding.btnWeek, binding.btnMonth);
            mIsWeek = true;
            refreshData();
        });

        binding.btnMonth.setOnClickListener(v -> {
            setPillActive(binding.btnMonth, binding.btnWeek);
            mIsWeek = false;
            refreshData();
        });
    }

    /**
     * 设置丸形切换的激活状态
     */
    private void setPillActive(TextView active, TextView inactive) {
        active.setBackgroundResource(R.drawable.bg_pill_active);
        active.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
        inactive.setBackgroundResource(android.R.color.transparent);
        inactive.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface_variant));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mDataLoaded) {
            refreshData();
        } else {
            mDataLoaded = true;
            mIsWeek = true;
            setPillActive(binding.btnWeek, binding.btnMonth);
            refreshData();
        }
    }

    private void refreshData() {
        if (binding == null) return;

        final boolean isWeek = mIsWeek;

        new Thread(() -> {
            RepositoryManager rm = RepositoryManager.getInstance();
            UserRepository userRepo = rm.getUserRepo();
            User currentUser = userRepo.getCurrentUser();
            if (currentUser == null) return;
            int userId = currentUser.getUserId();

            StatsRepository statsRepo = rm.getStatsRepo();
            HarvestRepository harvestRepo = rm.getHarvestRepo();

            String today = java.time.LocalDate.now().toString();
            int todayWatering = statsRepo.getTodayWateringCount(userId, today);
            int totalHarvest = harvestRepo.getTotalHarvestCount(userId);

            // 获取真实的能量记录
            List<EnergyRecord> records;
            if (isWeek) {
                records = getWeekEnergyRecords(userId);
            } else {
                records = getMonthEnergyRecords(userId);
            }

            // 收集折线图数据
            final List<Entry> lineEntries = new ArrayList<>();
            final List<String> xLabels = new ArrayList<>();
            EnergyRepository energyRepo = rm.getEnergyRepo();
            java.time.LocalDate todayDate = java.time.LocalDate.now();

            int days = isWeek ? 7 : java.time.YearMonth.now().lengthOfMonth();
            java.time.LocalDate startDate = isWeek
                    ? todayDate.with(java.time.DayOfWeek.MONDAY)
                    : todayDate.withDayOfMonth(1);
            int endDay = isWeek ? 7 : java.time.YearMonth.now().lengthOfMonth();

            for (int i = 0; i < endDay; i++) {
                java.time.LocalDate date = startDate.plusDays(i);
                if (date.isAfter(todayDate)) break;
                String dateStr = date.toString();
                int energy = energyRepo.getDailyNetEnergy(userId, dateStr);
                lineEntries.add(new Entry(i, Math.max(0, energy)));
                xLabels.add(dateStr.substring(5));
            }

            // 收集饼图数据：步数转换(0)、自己浇水消耗(1)、帮浇水消耗(3)
            int walkingEnergy = energyRepo.getEnergyBySourceType(userId, today, 0);
            int selfWatering = Math.abs(energyRepo.getEnergyBySourceType(userId, today, 1));
            int helpWatering = Math.abs(energyRepo.getEnergyBySourceType(userId, today, 3));

            final List<PieEntry> pieEntries = new ArrayList<>();
            if (walkingEnergy > 0) {
                pieEntries.add(new PieEntry(walkingEnergy, "步数转换"));
            }
            if (selfWatering > 0) {
                pieEntries.add(new PieEntry(selfWatering, "浇水消耗"));
            }
            if (helpWatering > 0) {
                pieEntries.add(new PieEntry(helpWatering, "帮浇水"));
            }
            final int pieSliceCount = pieEntries.size();
            final boolean usePercent = pieSliceCount > 0;
            if (pieEntries.isEmpty()) {
                pieEntries.add(new PieEntry(1f, "暂无数据"));
            }

            final int finalTodayWatering = todayWatering;
            final int finalTotalHarvest = totalHarvest;

            new Handler(Looper.getMainLooper()).post(() -> {
                if (binding == null) return;

                binding.tvTodayWatering.setText(String.valueOf(finalTodayWatering));
                binding.tvTotalHarvest.setText(String.valueOf(finalTotalHarvest));

                adapter.setRecords(records);

                // 设置折线图
                setupLineChart(lineEntries, xLabels);
                // 设置饼图
                setupPieChart(pieEntries, usePercent);
            });
        }).start();
    }

    /**
     * 配置能量趋势折线图（周/月），数据已在后台线程预计算
     */
    private void setupLineChart(List<Entry> entries, List<String> xLabels) {

        LineDataSet dataSet = new LineDataSet(entries, "每日能量");
        dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.primary));
        dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface_variant));
        dataSet.setValueTextSize(10f);
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.primary));
        dataSet.setCircleRadius(4f);
        dataSet.setFillColor(ColorTemplate.colorWithAlpha(
                ContextCompat.getColor(requireContext(), R.color.primary), 60));
        dataSet.setDrawFilled(true);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        // 样式
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface_variant));
        lineChart.getLegend().setTextSize(12f);
        lineChart.setDrawGridBackground(false);
        lineChart.setDrawBorders(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(false);
        lineChart.setPinchZoom(false);
        lineChart.setDoubleTapToZoomEnabled(false);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface_variant));
        xAxis.setTextSize(11f);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface_variant));
        leftAxis.setTextSize(11f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(ColorTemplate.colorWithAlpha(
                ContextCompat.getColor(requireContext(), R.color.outline_variant), 80));
        leftAxis.setAxisMinimum(0f);

        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);

        lineChart.invalidate();
    }

    /**
     * 配置能量来源分布饼图，数据已在后台线程预计算
     */
    private void setupPieChart(List<PieEntry> entries, boolean usePercent) {

        if (entries.isEmpty()) {
            entries.add(new PieEntry(1f, "暂无数据"));
            pieChart.setUsePercentValues(false);
        } else {
            pieChart.setUsePercentValues(true);
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        int primaryColor = ContextCompat.getColor(requireContext(), R.color.primary);
        int secondaryColor = ContextCompat.getColor(requireContext(), R.color.secondary);
        int grayColor = ColorTemplate.colorWithAlpha(
                ContextCompat.getColor(requireContext(), R.color.on_surface_variant), 80);

        if (entries.size() == 1 && entries.get(0).getLabel().equals("暂无数据")) {
            dataSet.setColors(new int[]{grayColor});
        } else if (entries.size() == 3) {
            dataSet.setColors(new int[]{primaryColor, secondaryColor,
                    ContextCompat.getColor(requireContext(), R.color.spend_red)});
        } else {
            dataSet.setColors(new int[]{primaryColor, secondaryColor});
        }

        dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.background));
        dataSet.setValueTextSize(13f);
        dataSet.setSliceSpace(entries.size() > 1 ? 4f : 0f);
        dataSet.setSelectionShift(5f);

        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new PercentFormatter(pieChart));
        pieChart.setUsePercentValues(true);
        pieChart.setData(pieData);

        // 样式
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setHoleRadius(50f);
        pieChart.setTransparentCircleRadius(55f);
        pieChart.setTransparentCircleColor(ColorTemplate.colorWithAlpha(
                ContextCompat.getColor(requireContext(), R.color.surface_container_low), 60));
        pieChart.setDrawCenterText(true);
        pieChart.setCenterText("能量来源");
        pieChart.setCenterTextSize(14f);
        pieChart.setCenterTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface_variant));
        pieChart.setRotationEnabled(true);
        pieChart.setRotationAngle(0);
        pieChart.setEntryLabelColor(ContextCompat.getColor(requireContext(), R.color.on_surface));
        pieChart.setEntryLabelTextSize(11f);
        pieChart.getLegend().setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface_variant));
        pieChart.getLegend().setTextSize(12f);
        pieChart.getLegend().setXEntrySpace(16f);
        pieChart.setExtraOffsets(0, 8, 0, 8);

        pieChart.invalidate();
    }

    /**
     * 获取本周能量记录（倒序：今天最新的在最上面）
     */
    private List<EnergyRecord> getWeekEnergyRecords(int userId) {
        List<EnergyRecord> all = new ArrayList<>();
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate monday = today.with(java.time.DayOfWeek.MONDAY);
        // 倒序遍历：今天 → 周一，保证最新记录在最前
        for (java.time.LocalDate d = today; !d.isBefore(monday); d = d.minusDays(1)) {
            all.addAll(com.sportytomato.app.model.database.AppDatabase
                    .getInstance(requireContext())
                    .energyRecordDao()
                    .getTodayEnergyRecords(userId, d.toString()));
        }
        return all;
    }

    /**
     * 获取本月能量记录（倒序：今天最新的在最上面）
     */
    private List<EnergyRecord> getMonthEnergyRecords(int userId) {
        List<EnergyRecord> all = new ArrayList<>();
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate firstOfMonth = today.withDayOfMonth(1);
        // 倒序遍历：今天 → 本月1号，保证最新记录在最前
        for (java.time.LocalDate d = today; !d.isBefore(firstOfMonth); d = d.minusDays(1)) {
            all.addAll(com.sportytomato.app.model.database.AppDatabase
                    .getInstance(requireContext())
                    .energyRecordDao()
                    .getTodayEnergyRecords(userId, d.toString()));
        }
        return all;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
