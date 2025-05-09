package com.example.recipeapp;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;
import java.util.concurrent.Executors;

@Database(entities = {Meal.class}, version = 1, exportSchema = false)
@TypeConverters(ListConverter.class)
public abstract class MealDatabase extends RoomDatabase {
    private static volatile MealDatabase INSTANCE;

    public abstract MealDao mealDao();

    public static MealDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (MealDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    MealDatabase.class, "meal_database")
                            .addCallback(new RoomDatabase.Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    Executors.newSingleThreadExecutor().execute(() -> {
                                        getDatabase(context).mealDao().insertAll(
                                                new Meal("breakfast"),
                                                new Meal("lunch"),
                                                new Meal("dinner"),
                                                new Meal("snacks")
                                        );
                                    });
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
