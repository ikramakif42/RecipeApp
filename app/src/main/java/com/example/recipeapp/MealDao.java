package com.example.recipeapp;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Transaction;

import java.util.ArrayList;
import java.util.List;

@Dao
public interface MealDao {
    // Single insert returns long (row ID)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Meal meal);

    // Multiple insert
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long[] insertAll(Meal... meals);

    @Update
    void update(Meal meal);

    @Query("SELECT * FROM meals WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    Meal getMealByName(String name);

    @Query("SELECT * FROM meals")
    List<Meal> getAllMeals();

    @Transaction
    default void upsert(Meal meal) {
        long id = insert(meal);
        if (id == -1) {
            update(meal);
        }
    }
    @Transaction
    default void removeIngredient(String mealName, String ingredient) {
        Meal meal = getMealByName(mealName);
        if (meal != null) {
            List<String> ingredients = new ArrayList<>(meal.getIngredients());
            ingredients.remove(ingredient.toLowerCase());
            meal.setIngredients(ingredients);
            update(meal);
        }
    }
}