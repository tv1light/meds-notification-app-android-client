package com.medreminder.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.medreminder.data.local.entity.ReminderEntity;
import com.medreminder.data.local.entity.ReminderWithCourseDrug;

import java.util.List;

@Dao
public interface ReminderDao {
    @Query("SELECT r.id AS reminderId, r.courseId AS courseId, c.drugId AS drugId, d.name AS drugName, c.dosageText AS dosageText, r.plannedDateTime AS plannedDateTime, r.status AS status, c.notificationType AS notificationType " +
            "FROM reminders r INNER JOIN therapy_courses c ON c.id = r.courseId " +
            "INNER JOIN drugs d ON d.id = c.drugId " +
            "WHERE r.plannedDateTime BETWEEN :dayStart AND :dayEnd ORDER BY r.plannedDateTime ASC")
    LiveData<List<ReminderWithCourseDrug>> observeForDay(long dayStart, long dayEnd);

    @Query("SELECT * FROM reminders WHERE id = :id")
    ReminderEntity getByIdSync(long id);

    @Query("SELECT * FROM reminders WHERE courseId = :courseId")
    List<ReminderEntity> getByCourseIdSync(long courseId);

    @Query("SELECT * FROM reminders WHERE status = :status AND plannedDateTime >= :fromTime ORDER BY plannedDateTime ASC")
    List<ReminderEntity> getByStatusFromTime(String status, long fromTime);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ReminderEntity reminder);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ReminderEntity> reminders);

    @Update
    void update(ReminderEntity reminder);

    @Query("DELETE FROM reminders WHERE courseId = :courseId")
    void deleteByCourseId(long courseId);

    @Query("UPDATE reminders SET status = :status, actualDateTime = :actualTime WHERE id = :reminderId")
    void updateStatus(long reminderId, String status, Long actualTime);

    @Query("UPDATE reminders SET plannedDateTime = :newPlannedTime, status = :status, actualDateTime = NULL, postponedFromReminderId = NULL WHERE id = :reminderId")
    void reschedule(long reminderId, long newPlannedTime, String status);
}
