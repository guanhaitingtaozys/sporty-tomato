package com.sportytomato.app.service;

import com.sportytomato.app.model.entity.User;
import com.sportytomato.app.repository.FriendRepository;
import com.sportytomato.app.repository.HarvestRepository;
import com.sportytomato.app.repository.UserRepository;
import java.util.*;

public class RankService {
    private final FriendRepository friendRepo;
    private final HarvestRepository harvestRepo;
    private final UserRepository userRepo;

    public RankService(FriendRepository friendRepo, HarvestRepository harvestRepo, UserRepository userRepo) {
        this.friendRepo = friendRepo; this.harvestRepo = harvestRepo; this.userRepo = userRepo;
    }

    public List<RankEntry> getDailyRanking(int userId) {
        Calendar cal = Calendar.getInstance(); cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);
        return buildRanking(userId, cal.getTimeInMillis());
    }
    public List<RankEntry> getWeeklyRanking(int userId) { Calendar cal = Calendar.getInstance(); cal.add(Calendar.DAY_OF_YEAR, -7); return buildRanking(userId, cal.getTimeInMillis()); }
    public List<RankEntry> getMonthlyRanking(int userId) { Calendar cal = Calendar.getInstance(); cal.add(Calendar.DAY_OF_YEAR, -30); return buildRanking(userId, cal.getTimeInMillis()); }

    private List<RankEntry> buildRanking(int userId, long sinceTimestamp) {
        List<Integer> allIds = new ArrayList<>(); allIds.add(userId); allIds.addAll(friendRepo.getFriendIds(userId));
        List<RankEntry> entries = new ArrayList<>();
        for (int id : allIds) { User user = userRepo.getUserById(id); if (user == null) continue; int count = harvestRepo.getPeriodHarvestCount(id, sinceTimestamp, System.currentTimeMillis()); entries.add(new RankEntry(user, count, 0)); }
        Collections.sort(entries, (a, b) -> Integer.compare(b.harvestCount, a.harvestCount));
        for (int i = 0; i < entries.size(); i++) entries.get(i).rank = i + 1;
        return entries;
    }

    public static class RankEntry { public final User user; public final int harvestCount; public int rank; public RankEntry(User user, int harvestCount, int rank) { this.user = user; this.harvestCount = harvestCount; this.rank = rank; } }
}