package com.medreminder.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.medreminder.data.local.entity.LocalProfileEntity;

@Dao
public interface LocalProfileDao {
    @Query("SELECT * FROM local_profiles WHERE login = :login LIMIT 1")
    LocalProfileEntity findByLoginSync(String login);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(LocalProfileEntity profile);
}
