// MealDatabase.java
package com.example.recipeapp;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {Meal.class}, version = 1, exportSchema = false)
@TypeConverters(ListConverter.class)
public abstract class MealDatabase extends RoomDatabase {
    private static MealDatabase instance;

    public abstract MealDao mealDao();

    public static synchronized MealDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            MealDatabase.class, "meal_database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}