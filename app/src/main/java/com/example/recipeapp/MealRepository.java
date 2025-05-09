package com.example.recipeapp;

import android.app.Application;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MealRepository {
        private final MealDao mealDao;
        private final Executor executor;
        public interface RepositoryCallback<T> {
            void onSuccess(T result);
            void onError(Exception e);
        }
        public MealRepository(Application application) {
            MealDatabase db = MealDatabase.getDatabase(application);
            mealDao = db.mealDao();
            executor = Executors.newSingleThreadExecutor();
        }

        public void addIngredientsToMeal(String mealName, List<String> ingredients) {
            executor.execute(() -> {
                try {
                    if (mealDao == null) {
                        throw new IllegalStateException("Database not initialized");
                    }

                    Meal meal = mealDao.getMealByName(mealName.toLowerCase());
                    if (meal == null) {
                        meal = new Meal(mealName.toLowerCase());
                        mealDao.insert(meal);
                        meal = mealDao.getMealByName(mealName.toLowerCase());
                    }

                    // Add ingredients
                    List<String> currentIngredients = new ArrayList<>(meal.getIngredients());
                    for (String ingredient : ingredients) {
                        String clean = ingredient.trim().toLowerCase();
                        if (!clean.isEmpty() && !currentIngredients.contains(clean)) {
                            currentIngredients.add(clean);
                        }
                    }

                    meal.setIngredients(currentIngredients);
                    mealDao.update(meal);

                } catch (Exception e) {
                    Log.e("REPO_ERROR", "Failed to add ingredients", e);
                }
            });
        }
    public void removeIngredient(String mealName, String ingredient, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                Meal meal = mealDao.getMealByName(mealName);
                if (meal != null) {
                    List<String> ingredients = new ArrayList<>(meal.getIngredients());
                    if (ingredients.remove(ingredient.toLowerCase())) {
                        meal.setIngredients(ingredients);
                        mealDao.update(meal);
                        callback.onSuccess(null);
                    } else {
                        callback.onError(new Exception("Ingredient not found"));
                    }
                } else {
                    callback.onError(new Exception("Meal category not found"));
                }
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
}