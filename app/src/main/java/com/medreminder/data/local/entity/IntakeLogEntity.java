package com.medreminder.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "intake_logs", indices = {@Index("courseId"), @Index("drugId"), @Index("plannedDateTime")})
public class IntakeLogEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long courseId;

    public long drugId;

    public long plannedDateTime;

    public Long actualDateTime;

    @NonNull
    public String status = "PLANNED";

    public String comment;
}
