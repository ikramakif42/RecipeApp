package com.example.recipeapp;

import androidx.room.TypeConverter;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;

public class ListConverter {
    @TypeConverter
    public static List<String> fromString(String value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(value.split(","));
    }

    @TypeConverter
    public static String fromList(List<String> list) {
        return list == null || list.isEmpty() ? "" : String.join(",", list);
    }
}