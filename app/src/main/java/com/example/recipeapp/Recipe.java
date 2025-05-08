package com.example.recipeapp;

public class Recipe {
    private String id, title, calorieInfo, servingInfo, ingredients, instructions;

    public Recipe(String id, String title, String calorieInfo, String servingInfo, String ingredients, String instructions) {
        this.id = id;
        this.title = title;
        this.calorieInfo = calorieInfo;
        this.servingInfo = servingInfo;
        this.ingredients = ingredients;
        this.instructions = instructions;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCalorieInfo() {
        return calorieInfo;
    }

    public void setCalorieInfo(String calorieInfo) {
        this.calorieInfo = calorieInfo;
    }

    public String getServingInfo() {
        return servingInfo;
    }

    public void setServingInfo(String servingInfo) {
        this.servingInfo = servingInfo;
    }

    public String getIngredients() {
        return ingredients;
    }

    public void setIngredients(String ingredients) {
        this.ingredients = ingredients;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    @Override
    public String toString() {
        return "Recipe{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", calorieInfo='" + calorieInfo + '\'' +
                ", servingInfo='" + servingInfo + '\'' +
                ", ingredients='" + ingredients + '\'' +
                ", instructions='" + instructions + '\'' +
                '}';
    }
}
