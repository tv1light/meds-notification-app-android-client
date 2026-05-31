package com.medreminder.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "drugs", indices = {@Index(value = {"serverId"}, unique = true), @Index(value = {"name"})})
public class DrugEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long serverId;

    @NonNull
    public String name = "";

    @NonNull
    public String form = "";

    @NonNull
    public String dosage = "";

    @NonNull
    public String activeSubstance = "";

    @NonNull
    public String country = "";

    @NonNull
    public String manufacturer = "";

    public long updatedAt;

    public static DrugEntity createSeed(long serverId,
                                        String name,
                                        String form,
                                        String dosage,
                                        String activeSubstance,
                                        String country,
                                        String manufacturer,
                                        long updatedAt) {
        DrugEntity entity = new DrugEntity();
        entity.serverId = serverId;
        entity.name = name;
        entity.form = form;
        entity.dosage = dosage;
        entity.activeSubstance = activeSubstance;
        entity.country = country;
        entity.manufacturer = manufacturer;
        entity.updatedAt = updatedAt;
        return entity;
    }
}
