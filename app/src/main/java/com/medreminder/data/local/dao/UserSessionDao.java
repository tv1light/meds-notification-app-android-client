package com.medreminder.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.medreminder.data.local.entity.UserSessionEntity;

@Dao
public interface UserSessionDao {
    @Query("SELECT * FROM user_session WHERE id = 1")
    UserSessionEntity getSessionSync();

    @Query("SELECT * FROM user_session WHERE id = 1")
    LiveData<UserSessionEntity> observeSession();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(UserSessionEntity session);

    @Query("DELETE FROM user_session")
    void clear();
}
