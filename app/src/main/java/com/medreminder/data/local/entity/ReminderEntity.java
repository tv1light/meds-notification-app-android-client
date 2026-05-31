package com.medreminder.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "reminders", indices = {@Index("courseId"), @Index("plannedDateTime")})
public class ReminderEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long courseId;

    public long plannedDateTime;

    public Long actualDateTime;

    @NonNull
    public String status = "PLANNED";

    public Long postponedFromReminderId;
}
