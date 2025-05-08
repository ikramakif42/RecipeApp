package com.example.recipeapp;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.example.recipeapp.Meal;
import com.example.recipeapp.MealDatabase;
import java.util.List; // Add this import
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements BotChat.ApiResponseListener {
    private ChatAdapter chatAdapter;
    private ArrayList<ChatMessage> chatMessages;
    private EditText editTextMessage;
    public static ArrayList<JSONObject> conversationHistory = new ArrayList<>();
    private MealDatabase mealDb;
    private List<Meal> meals = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);

        editTextMessage = findViewById(R.id.editTextMessage);
        findViewById(R.id.btnSend).setOnClickListener(this::sendMessage);
        findViewById(R.id.btnOptions).setOnClickListener(this::viewOptions);

        mealDb = MealDatabase.getInstance(this);
        loadInitialMeals();
    }
    private void loadInitialMeals() {
        new Thread(() -> {
            List<Meal> storedMeals = mealDb.mealDao().getAllMeals();
            if (storedMeals.isEmpty()) {
                // Create default meals if none exist
                Meal breakfast = new Meal("Breakfast");
                Meal lunch = new Meal("Lunch");
                Meal dinner = new Meal("Dinner");
                Meal snacks = new Meal("Snacks");

                mealDb.mealDao().insert(breakfast);
                mealDb.mealDao().insert(lunch);
                mealDb.mealDao().insert(dinner);
                mealDb.mealDao().insert(snacks);

                storedMeals = mealDb.mealDao().getAllMeals();
            }
            meals = storedMeals;
        }).start();
    }

    private void showMealsDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_layout);

        RecyclerView mealsRecyclerView = dialog.findViewById(R.id.mealsRecyclerView);
        mealsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mealsRecyclerView.setAdapter(new MealAdapter(meals));

        dialog.findViewById(R.id.btnCloseMeals).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
    private void viewOptions(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.options_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            String selected = (String) item.getTitle();

            if (selected.equals("Load Preset") ||
                    selected.equals("Remove Presets") ||
                    selected.equals("View Favorites") ||
                    selected.equals("Delete Favorites")) {

                showDummyDialog(selected);
                return true;
            }
            if (selected.equals("Meals")) {
                showMealsDialog();
                return true;
            }
            return false;
        });

        popup.show();
    }
    public void addIngredientsToMeal(String mealName, List<String> newIngredients) {
        new Thread(() -> {
            try {
                Meal meal = mealDb.mealDao().getMealByName(mealName);
                if (meal == null) {
                    meal = new Meal(mealName);
                    mealDb.mealDao().insert(meal);
                    meal = mealDb.mealDao().getMealByName(mealName);
                }

                List<String> currentIngredients = new ArrayList<>(meal.getIngredients());
                for (String ingredient : newIngredients) {
                    String cleanIngredient = ingredient.trim().toLowerCase();
                    if (!cleanIngredient.isEmpty() && !currentIngredients.contains(cleanIngredient)) {
                        currentIngredients.add(cleanIngredient);
                    }
                }

                if (!currentIngredients.equals(meal.getIngredients())) {
                    meal.setIngredients(currentIngredients);
                    mealDb.mealDao().update(meal);
                    Log.d("DB_UPDATE", "Updated " + mealName + " with: " + currentIngredients);
                }

                runOnUiThread(() -> {
                    Toast.makeText(this, "✓ Added to " + mealName, Toast.LENGTH_SHORT).show();
                    debugPrintAllMeals();
                });

            } catch (Exception e) {
                Log.e("DB_ERROR", "Failed to update meal", e);
                runOnUiThread(() ->
                        Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void loadMeals() {
        new Thread(() -> {
            List<Meal> meals = mealDb.mealDao().getAllMeals();
            runOnUiThread(() -> {
            });
        }).start();
    }
    private void sendMessage(View v) {
        if (findViewById(R.id.introMessage).getVisibility() == View.VISIBLE) {
            findViewById(R.id.introMessage).setVisibility(View.GONE);
        }

        String message = editTextMessage.getText().toString().trim();
        if (message.isEmpty()) return;

        chatMessages.add(new ChatMessage(message, true));
        editTextMessage.setText("");
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        new BotChat(this).execute(message);
    }

    private void showDummyDialog(String title) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_dummy_popup);
        dialog.setCancelable(true);

        TextView titleView = dialog.findViewById(R.id.dialogTitle);
        titleView.setText(title);

        dialog.findViewById(R.id.btnCloseDialog).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
    private void debugPrintAllMeals() {
        new Thread(() -> {
            List<Meal> allMeals = mealDb.mealDao().getAllMeals();
            for (Meal meal : allMeals) {
                Log.d("MEAL_DEBUG", "Meal: " + meal.getName() +
                        " | Ingredients: " + String.join(", ", meal.getIngredients()));
            }
        }).start();
    }


    @Override
    public void onApiResponse(String response) {
        chatMessages.add(new ChatMessage(response, false));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
    }
}