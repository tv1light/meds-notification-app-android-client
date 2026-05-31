package com.medreminder.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "app_settings")
public class AppSettingsEntity {
    @PrimaryKey
    public int id = 1;

    @NonNull
    public String serverUrl = "";

    public boolean quietModeEnabled;

    @NonNull
    public String quietModeStart = "22:00";

    @NonNull
    public String quietModeEnd = "07:00";
}
