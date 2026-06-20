package com.sportytomato.app.model.database;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.sportytomato.app.model.dao.BadgeDao;
import com.sportytomato.app.model.dao.EnergyRecordDao;
import com.sportytomato.app.model.dao.FriendDao;
import com.sportytomato.app.model.dao.HarvestLogDao;
import com.sportytomato.app.model.dao.MergeLogDao;
import com.sportytomato.app.model.dao.MessageDao;
import com.sportytomato.app.model.dao.StepRecordDao;
import com.sportytomato.app.model.dao.SyncQueueDao;
import com.sportytomato.app.model.dao.TomatoDao;
import com.sportytomato.app.model.dao.UserDao;
import com.sportytomato.app.model.dao.WateringLogDao;
import com.sportytomato.app.model.entity.*;

@Database(entities = {User.class, TomatoState.class, StepRecord.class, EnergyRecord.class, WateringLog.class, HarvestLog.class, BadgeTemplate.class, UserBadge.class, Friendship.class, SyncQueue.class, MergeLog.class, Message.class}, version = 8, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;
    public abstract UserDao userDao();
    public abstract TomatoDao tomatoDao();
    public abstract StepRecordDao stepRecordDao();
    public abstract EnergyRecordDao energyRecordDao();
    public abstract WateringLogDao wateringLogDao();
    public abstract HarvestLogDao harvestLogDao();
    public abstract BadgeDao badgeDao();
    public abstract FriendDao friendDao();
    public abstract SyncQueueDao syncQueueDao();
    public abstract MergeLogDao mergeLogDao();
    public abstract MessageDao messageDao();

    public static AppDatabase getInstance(@NonNull Context context) {
        if (INSTANCE == null) { synchronized (AppDatabase.class) { if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "sporty_tomato.db")
                .allowMainThreadQueries().setQueryExecutor(java.util.concurrent.Executors.newSingleThreadExecutor())
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8).build();
        } } }
        return INSTANCE;
    }

    static final Migration MIGRATION_1_2 = new Migration(1, 2) { @Override public void migrate(@NonNull SupportSQLiteDatabase database) {} };
    static final Migration MIGRATION_2_3 = new Migration(2, 3) { @Override public void migrate(@NonNull SupportSQLiteDatabase database) {} };
    static final Migration MIGRATION_3_4 = new Migration(3, 4) { @Override public void migrate(@NonNull SupportSQLiteDatabase database) {} };
    static final Migration MIGRATION_4_5 = new Migration(4, 5) { @Override public void migrate(@NonNull SupportSQLiteDatabase database) { database.execSQL("ALTER TABLE users ADD COLUMN phone TEXT"); database.execSQL("ALTER TABLE users ADD COLUMN passwordHash TEXT"); database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_users_phone ON users (phone)"); } };
    static final Migration MIGRATION_5_6 = new Migration(5, 6) { @Override public void migrate(@NonNull SupportSQLiteDatabase database) { database.execSQL("CREATE TABLE IF NOT EXISTS sync_queue (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, userId INTEGER NOT NULL, action TEXT, collection TEXT, payload TEXT, status INTEGER NOT NULL DEFAULT 0, retryCount INTEGER NOT NULL DEFAULT 0, nextRetryAt INTEGER NOT NULL DEFAULT 0, createdAt INTEGER NOT NULL DEFAULT 0, lastError TEXT)"); database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_userId ON sync_queue (userId)"); database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_status ON sync_queue (status)"); database.execSQL("CREATE TABLE IF NOT EXISTS merge_logs (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, userId INTEGER NOT NULL, collection TEXT, localSnapshot TEXT, remoteSnapshot TEXT, mergedSnapshot TEXT, strategy TEXT, mergedAt INTEGER NOT NULL DEFAULT 0)"); database.execSQL("CREATE INDEX IF NOT EXISTS index_merge_logs_userId ON merge_logs (userId)"); database.execSQL("CREATE INDEX IF NOT EXISTS index_merge_logs_mergedAt ON merge_logs (mergedAt)"); database.execSQL("ALTER TABLE watering_logs ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0"); database.execSQL("ALTER TABLE tomatoes ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0"); database.execSQL("CREATE INDEX IF NOT EXISTS index_tomatoes_isDeleted ON tomatoes (isDeleted)"); } };
    static final Migration MIGRATION_6_7 = new Migration(6, 7) { @Override public void migrate(@NonNull SupportSQLiteDatabase database) { database.execSQL("CREATE TABLE IF NOT EXISTS messages (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, fromUserId INTEGER NOT NULL, toUserId INTEGER NOT NULL, content TEXT, type INTEGER NOT NULL DEFAULT 0, timestamp INTEGER NOT NULL DEFAULT 0, read INTEGER NOT NULL DEFAULT 0)"); database.execSQL("CREATE INDEX IF NOT EXISTS index_messages_fromUserId_toUserId ON messages (fromUserId, toUserId)"); database.execSQL("CREATE INDEX IF NOT EXISTS index_messages_timestamp ON messages (timestamp)"); } };
    static final Migration MIGRATION_7_8 = new Migration(7, 8) { @Override public void migrate(@NonNull SupportSQLiteDatabase database) { database.execSQL("ALTER TABLE tomatoes ADD COLUMN variety INTEGER NOT NULL DEFAULT 0"); database.execSQL("ALTER TABLE tomatoes ADD COLUMN varietyUnlockedAt INTEGER NOT NULL DEFAULT 0"); } };
}