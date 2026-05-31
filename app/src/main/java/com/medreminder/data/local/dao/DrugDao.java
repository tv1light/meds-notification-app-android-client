package com.medreminder.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.medreminder.data.local.entity.DrugEntity;

import java.util.List;

@Dao
public interface DrugDao {
    @Query("SELECT * FROM drugs ORDER BY name COLLATE NOCASE ASC")
    LiveData<List<DrugEntity>> observeAll();

    @Query("SELECT * FROM drugs WHERE name LIKE '%' || :query || '%' ORDER BY name COLLATE NOCASE ASC")
    LiveData<List<DrugEntity>> search(String query);

    @Query("SELECT * FROM drugs ORDER BY name COLLATE NOCASE ASC")
    List<DrugEntity> getAllSync();

    @Query("SELECT * FROM drugs WHERE id = :id")
    DrugEntity getByIdSync(long id);

    @Query("SELECT COUNT(*) FROM drugs")
    long countAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<DrugEntity> drugs);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(DrugEntity drug);

    @Query("DELETE FROM drugs")
    void clearAll();
}
