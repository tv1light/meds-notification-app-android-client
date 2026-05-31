package com.medreminder.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "local_profiles",
        indices = {@Index(value = {"login"}, unique = true)}
)
public class LocalProfileEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String login = "";

    @NonNull
    public String password = "";

    public long createdAt;
}
