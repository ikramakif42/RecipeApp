package com.example.recipeapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MealAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<Meal> meals;
    private static final int EMPTY_VIEW = 0;
    private static final int MEAL_VIEW = 1;

    public MealAdapter(List<Meal> meals) {
        this.meals = meals;
    }

    @Override
    public int getItemViewType(int position) {
        return meals.isEmpty() ? EMPTY_VIEW : MEAL_VIEW;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == EMPTY_VIEW) {
            View view = inflater.inflate(R.layout.item_empty_state, parent, false);
            return new EmptyViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_meal, parent, false);
            return new MealViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof MealViewHolder) {
            Meal meal = meals.get(position);
            MealViewHolder mealHolder = (MealViewHolder) holder;

            mealHolder.mealName.setText(meal.getName());

            if (meal.getIngredients().isEmpty()) {
                mealHolder.mealIngredients.setText("No ingredients added yet");
            } else {
                mealHolder.mealIngredients.setText(String.join(", ", meal.getIngredients()));
            }
        } else if (holder instanceof EmptyViewHolder) {
            EmptyViewHolder emptyHolder = (EmptyViewHolder) holder;
            emptyHolder.emptyText.setText("No meal categories available");
        }
    }

    @Override
    public int getItemCount() {
        return meals.isEmpty() ? 1 : meals.size(); // Return 1 for empty view
    }

    static class MealViewHolder extends RecyclerView.ViewHolder {
        TextView mealName;
        TextView mealIngredients;

        MealViewHolder(View itemView) {
            super(itemView);
            mealName = itemView.findViewById(R.id.mealName);
            mealIngredients = itemView.findViewById(R.id.mealIngredients);
        }
    }

    static class EmptyViewHolder extends RecyclerView.ViewHolder {
        TextView emptyText;

        EmptyViewHolder(View itemView) {
            super(itemView);
            emptyText = itemView.findViewById(R.id.emptyText);
        }
    }

    public void updateMeals(List<Meal> newMeals) {
        this.meals = newMeals;
        notifyDataSetChanged();
    }
}