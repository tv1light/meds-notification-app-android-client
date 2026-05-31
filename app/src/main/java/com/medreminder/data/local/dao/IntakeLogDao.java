package com.medreminder.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.medreminder.data.local.entity.IntakeLogEntity;
import com.medreminder.data.local.entity.IntakeLogWithDrug;

import java.util.List;

@Dao
public interface IntakeLogDao {
    @Query("SELECT l.id AS id, l.courseId AS courseId, l.drugId AS drugId, l.plannedDateTime AS plannedDateTime, l.actualDateTime AS actualDateTime, l.status AS status, l.comment AS comment, d.name AS drugName " +
            "FROM intake_logs l LEFT JOIN drugs d ON d.id = l.drugId " +
            "WHERE (:fromTs = 0 OR l.plannedDateTime >= :fromTs) " +
            "AND (:toTs = 0 OR l.plannedDateTime <= :toTs) " +
            "AND (:drugQuery = '' OR d.name LIKE '%' || :drugQuery || '%') " +
            "ORDER BY l.plannedDateTime DESC")
    LiveData<List<IntakeLogWithDrug>> observeFiltered(long fromTs, long toTs, String drugQuery);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(IntakeLogEntity log);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<IntakeLogEntity> logs);

    @Query("DELETE FROM intake_logs WHERE id IN (" +
            "SELECT id FROM intake_logs " +
            "WHERE courseId = :courseId AND drugId = :drugId AND plannedDateTime = :plannedDateTime AND status = :status " +
            "ORDER BY COALESCE(actualDateTime, 0) DESC, id DESC LIMIT 1)")
    void deleteLastByReminderKeys(long courseId, long drugId, long plannedDateTime, String status);
}
