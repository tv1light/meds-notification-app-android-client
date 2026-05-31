package com.medreminder.util;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.medreminder.data.local.entity.DrugEntity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class SeedDataUtil {
    private SeedDataUtil() {}

    public static List<DrugEntity> loadSeedDrugs(Context context) {
        List<DrugEntity> fromAsset = loadFromAsset(context);
        if (!fromAsset.isEmpty()) {
            return fromAsset;
        }
        return fallback();
    }

    private static List<DrugEntity> loadFromAsset(Context context) {
        List<DrugEntity> result = new ArrayList<>();
        try (InputStream is = context.getAssets().open("drugs_seed.json");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            Type type = new TypeToken<List<DrugEntity>>() {}.getType();
            List<DrugEntity> parsed = new Gson().fromJson(reader, type);
            if (parsed != null) {
                long now = System.currentTimeMillis();
                for (DrugEntity drug : parsed) {
                    drug.updatedAt = now;
                    result.add(drug);
                }
            }
        } catch (IOException ignored) {
        }
        return result;
    }

    private static List<DrugEntity> fallback() {
        long now = System.currentTimeMillis();
        List<DrugEntity> list = new ArrayList<>();
        list.add(DrugEntity.createSeed(1, "Парацетамол", "таблетки", "500 мг", "Парацетамол", "Россия", "Фармстандарт", now));
        list.add(DrugEntity.createSeed(2, "Ибупрофен", "капсулы", "200 мг", "Ибупрофен", "Германия", "Berlin-Chemie", now));
        list.add(DrugEntity.createSeed(3, "Амоксициллин", "таблетки", "500 мг", "Амоксициллин", "Сербия", "Hemofarm", now));
        list.add(DrugEntity.createSeed(4, "Омепразол", "капсулы", "20 мг", "Омепразол", "Россия", "Озон", now));
        return list;
    }
}
