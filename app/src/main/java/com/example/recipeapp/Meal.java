package com.example.recipeapp;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.recipeapp.ListConverter;

import java.util.List;
import java.util.ArrayList;

@Entity(tableName = "meals")
@TypeConverters(ListConverter.class)
public class Meal {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @Override
    public String toString() {
        return name + ": " + String.join(", ", ingredients);
    }
    private String name;
    private List<String> ingredients;

    public Meal(String name) {
        this.name = name;
        this.ingredients = new ArrayList<>();
    }

    // Required getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<String> getIngredients() { return ingredients; }
    public void setIngredients(List<String> ingredients) {
        this.ingredients = ingredients;
    }
}