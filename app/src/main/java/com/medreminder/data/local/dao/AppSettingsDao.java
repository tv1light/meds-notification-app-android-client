package com.medreminder.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.medreminder.data.local.entity.AppSettingsEntity;

@Dao
public interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    AppSettingsEntity getSync();

    @Query("SELECT * FROM app_settings WHERE id = 1")
    LiveData<AppSettingsEntity> observe();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(AppSettingsEntity settings);
}
