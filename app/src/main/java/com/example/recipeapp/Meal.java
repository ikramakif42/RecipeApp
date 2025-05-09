package com.example.recipeapp;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import androidx.annotation.NonNull;
import java.util.List;
import java.util.ArrayList;

@Entity(tableName = "meals",
        indices = {@Index(value = {"name"}, unique = true)})
@TypeConverters(ListConverter.class)
public class Meal {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "name")
    @NonNull
    private String name;

    @ColumnInfo(name = "ingredients")
    private List<String> ingredients;

    public Meal(@NonNull String name) {
        this.name = name.toLowerCase();
        this.ingredients = new ArrayList<>();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    @NonNull
    public String getName() { return name; }
    public void setName(@NonNull String name) { this.name = name.toLowerCase(); }

    public List<String> getIngredients() { return ingredients; }
    public void setIngredients(List<String> ingredients) {
        this.ingredients = ingredients != null ? ingredients : new ArrayList<>();
    }

    public void addIngredient(String ingredient) {
        String clean = ingredient.trim().toLowerCase();
        if (!clean.isEmpty() && !ingredients.contains(clean)) {
            ingredients.add(clean);
        }
    }
}