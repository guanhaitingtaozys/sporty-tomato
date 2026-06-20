package com.sportytomato.app.service;

import android.content.Context;
import com.sportytomato.app.model.database.AppDatabase;
import com.sportytomato.app.repository.*;
import com.sportytomato.app.repository.emas.*;
import com.sportytomato.app.repository.mock.*;

public class RepositoryManager {
    public enum DataSource { ROOM, MOCK, CLOUD, EMAS }
    private static volatile RepositoryManager instance;
    private DataSource currentSource = DataSource.ROOM;
    private volatile boolean roomInitialized = false;
    private UserRepository roomUserRepo, mockUserRepo, emasUserRepo;
    private EnergyRepository roomEnergyRepo, mockEnergyRepo, emasEnergyRepo;
    private TomatoRepository roomTomatoRepo, mockTomatoRepo, emasTomatoRepo;
    private HarvestRepository roomHarvestRepo, mockHarvestRepo, emasHarvestRepo;
    private StatsRepository roomStatsRepo, mockStatsRepo, emasStatsRepo;
    private FriendRepository roomFriendRepo, mockFriendRepo, emasFriendRepo;
    private MessageRepository roomMessageRepo;

    private RepositoryManager() {}

    public static RepositoryManager getInstance() {
        if (instance == null) { synchronized (RepositoryManager.class) { if (instance == null) instance = new RepositoryManager(); } }
        return instance;
    }

    public synchronized void init(AppDatabase database, Context context) {
        if (roomInitialized) return;
        roomUserRepo = new UserRepositoryImpl(database, context);
        roomEnergyRepo = new EnergyRepositoryImpl(database);
        roomTomatoRepo = new TomatoRepositoryImpl(database);
        roomHarvestRepo = new HarvestRepositoryImpl(database);
        roomStatsRepo = new StatsRepositoryImpl(database);
        roomFriendRepo = new FriendRepositoryImpl(database);
        roomMessageRepo = new MessageRepositoryImpl(database, roomUserRepo);
        roomInitialized = true;
    }

    public void setDataSource(DataSource source) { this.currentSource = source; }
    public DataSource getCurrentSource() { return currentSource; }
    public UserRepository getUserRepo() { return roomUserRepo; }
    public EnergyRepository getEnergyRepo() { return currentSource == DataSource.MOCK ? getOrCreateMockEnergyRepo() : currentSource == DataSource.EMAS ? getOrCreateEMASEnergyRepo() : roomEnergyRepo; }
    public TomatoRepository getTomatoRepo() { return currentSource == DataSource.MOCK ? getOrCreateMockTomatoRepo() : currentSource == DataSource.EMAS ? getOrCreateEMASTomatoRepo() : roomTomatoRepo; }
    public HarvestRepository getHarvestRepo() { return currentSource == DataSource.MOCK ? getOrCreateMockHarvestRepo() : currentSource == DataSource.EMAS ? getOrCreateEMASHarvestRepo() : roomHarvestRepo; }
    public StatsRepository getStatsRepo() { return currentSource == DataSource.MOCK ? getOrCreateMockStatsRepo() : currentSource == DataSource.EMAS ? getOrCreateEMASStatsRepo() : roomStatsRepo; }
    public FriendRepository getFriendRepo() { return currentSource == DataSource.MOCK ? getOrCreateMockFriendRepo() : currentSource == DataSource.EMAS ? getOrCreateEMASFriendRepo() : roomFriendRepo; }
    public MessageRepository getMessageRepo() { return roomMessageRepo; }

    private EnergyRepository getOrCreateMockEnergyRepo() { if (mockEnergyRepo == null) mockEnergyRepo = new MockEnergyRepository(); return mockEnergyRepo; }
    private TomatoRepository getOrCreateMockTomatoRepo() { if (mockTomatoRepo == null) mockTomatoRepo = new MockTomatoRepository(); return mockTomatoRepo; }
    private HarvestRepository getOrCreateMockHarvestRepo() { if (mockHarvestRepo == null) mockHarvestRepo = new MockHarvestRepository(); return mockHarvestRepo; }
    private StatsRepository getOrCreateMockStatsRepo() { if (mockStatsRepo == null) mockStatsRepo = new MockStatsRepository((MockTomatoRepository)getOrCreateMockTomatoRepo(), (MockHarvestRepository)getOrCreateMockHarvestRepo()); return mockStatsRepo; }
    private FriendRepository getOrCreateMockFriendRepo() { if (mockFriendRepo == null) mockFriendRepo = new MockFriendRepository((MockUserRepository)mockUserRepo); return mockFriendRepo; }
    private EnergyRepository getOrCreateEMASEnergyRepo() { if (emasEnergyRepo == null) emasEnergyRepo = new EMASEnergyRepository(); return emasEnergyRepo; }
    private TomatoRepository getOrCreateEMASTomatoRepo() { if (emasTomatoRepo == null) emasTomatoRepo = new EMASTomatoRepository(); return emasTomatoRepo; }
    private HarvestRepository getOrCreateEMASHarvestRepo() { if (emasHarvestRepo == null) emasHarvestRepo = new EMASHarvestRepository(); return emasHarvestRepo; }
    private StatsRepository getOrCreateEMASStatsRepo() { if (emasStatsRepo == null) emasStatsRepo = new EMASStatsRepository(); return emasStatsRepo; }
    private FriendRepository getOrCreateEMASFriendRepo() { if (emasFriendRepo == null) emasFriendRepo = new EMASFriendRepository(); return emasFriendRepo; }

    public void shutdown() { if (roomUserRepo instanceof UserRepositoryImpl) ((UserRepositoryImpl) roomUserRepo).shutdown(); if (roomEnergyRepo instanceof EnergyRepositoryImpl) ((EnergyRepositoryImpl) roomEnergyRepo).shutdown(); }
}