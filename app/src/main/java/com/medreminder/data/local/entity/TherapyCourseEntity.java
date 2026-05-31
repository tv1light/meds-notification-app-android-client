package com.medreminder.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "therapy_courses", indices = {@Index("drugId")})
public class TherapyCourseEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long drugId;

    @NonNull
    public String dosageText = "";

    public long startDate;

    public long endDate;

    @NonNull
    public String scheduleType = "EXACT_TIMES";

    @NonNull
    public String notificationType = "BANNER";

    public boolean isActive;

    public long createdAt;

    @NonNull
    public String exactTimesCsv = "08:00,20:00";

    public int intervalMinutes = 0;

    public int courseDays = 0;

    public int pauseDays = 0;

    public int notificationLeadMinutes = 0;
}
