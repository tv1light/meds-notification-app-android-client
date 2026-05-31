package com.medreminder.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.medreminder.data.local.entity.CourseWithDrug;
import com.medreminder.data.local.entity.TherapyCourseEntity;

import java.util.List;

@Dao
public interface TherapyCourseDao {
    @Query("SELECT c.id AS courseId, c.drugId AS drugId, d.name AS drugName, c.dosageText AS dosageText, c.startDate AS startDate, c.endDate AS endDate, c.scheduleType AS scheduleType, c.isActive AS isActive " +
            "FROM therapy_courses c INNER JOIN drugs d ON d.id = c.drugId " +
            "WHERE d.name LIKE '%' || :search || '%' ORDER BY c.startDate DESC")
    LiveData<List<CourseWithDrug>> observeByStartDate(String search);

    @Query("SELECT c.id AS courseId, c.drugId AS drugId, d.name AS drugName, c.dosageText AS dosageText, c.startDate AS startDate, c.endDate AS endDate, c.scheduleType AS scheduleType, c.isActive AS isActive " +
            "FROM therapy_courses c INNER JOIN drugs d ON d.id = c.drugId " +
            "WHERE d.name LIKE '%' || :search || '%' ORDER BY d.name COLLATE NOCASE ASC")
    LiveData<List<CourseWithDrug>> observeByDrugName(String search);

    @Query("SELECT * FROM therapy_courses WHERE id = :id")
    TherapyCourseEntity getByIdSync(long id);

    @Query("SELECT * FROM therapy_courses WHERE isActive = 1")
    List<TherapyCourseEntity> getActiveSync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(TherapyCourseEntity course);

    @Update
    void update(TherapyCourseEntity course);

    @Delete
    void delete(TherapyCourseEntity course);
}
