package com.example.recipeapp;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface MealDao {
    @Insert
    void insert(com.example.recipeapp.Meal meal);

    @Update
    void update(com.example.recipeapp.Meal meal);

    @Query("SELECT * FROM meals WHERE name = :name LIMIT 1")
    com.example.recipeapp.Meal getMealByName(String name);

    @Query("SELECT * FROM meals")
    List<com.example.recipeapp.Meal> getAllMeals();
}