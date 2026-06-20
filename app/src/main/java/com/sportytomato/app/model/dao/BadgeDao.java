package com.sportytomato.app.model.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.sportytomato.app.model.entity.BadgeTemplate;
import com.sportytomato.app.model.entity.UserBadge;

import java.util.List;

/**
 * 勋章数据访问对象
 */
@Dao
public interface BadgeDao {

    /** 获取所有勋章模板 */
    @Query("SELECT * FROM badge_templates")
    List<BadgeTemplate> getAllBadges();

    /** 获取指定用户已解锁的勋章 */
    @Query("SELECT * FROM user_badges WHERE userId = :userId")
    List<UserBadge> getUserBadges(int userId);

    /** 获取用户特定勋章（用于判断是否已解锁） */
    @Query("SELECT * FROM user_badges WHERE userId = :userId AND badgeId = :badgeId LIMIT 1")
    UserBadge getUserBadge(int userId, int badgeId);

    /** 插入用户勋章记录 */
    @Insert
    void insertBadge(UserBadge badge);

    /** 插入勋章模板 */
    @Insert
    void insertBadgeTemplate(BadgeTemplate template);

    /** 删除用户所有勋章记录（用于恢复云端数据前清除本地旧数据） */
    @Query("DELETE FROM user_badges WHERE userId = :userId")
    void deleteByUserId(int userId);

    /** 获取用户所有勋章（含解锁时间） */
    @Query("SELECT * FROM user_badges WHERE userId = :userId")
    List<UserBadge> getAllUserBadges(int userId);
}
