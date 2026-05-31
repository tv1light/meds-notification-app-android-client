package com.medreminder.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_session")
public class UserSessionEntity {
    @PrimaryKey
    public int id = 1;

    public long userId;

    @NonNull
    public String login = "";

    @NonNull
    public String role = "USER";

    public String token;

    @NonNull
    public String serverUrl = "";

    public boolean isLoggedIn;
}
