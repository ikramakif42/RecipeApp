package com.example.recipeapp;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.util.ArrayList;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog; // <-- This is the missing one
import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;
import java.util.List;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements ApiResponseListener {
    private ChatAdapter chatAdapter;
    private ArrayList<ChatMessage> chatMessages;
    private EditText editTextMessage;
    public static ArrayList<JSONObject> conversationHistory = new ArrayList<>();
    public static MealDatabase mealDb;
    private List<Meal> meals = new ArrayList<>();
    public static ArrayList<Recipe> recipeList = new ArrayList<>();
    private static MealRepository mealRepository;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DBHelper(this);


        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);

        editTextMessage = findViewById(R.id.editTextMessage);
        findViewById(R.id.btnSend).setOnClickListener(this::sendMessage);
        findViewById(R.id.btnOptions).setOnClickListener(this::viewOptions);

        mealDb = MealDatabase.getDatabase(getApplicationContext());
        mealRepository = new MealRepository(getApplication());
        loadInitialMeals();
    }

    private void loadInitialMeals() {
        new Thread(() -> {
            try {
                if (mealDb == null) {
                    mealDb = MealDatabase.getDatabase(getApplicationContext());
                }

                if (mealDb.mealDao().getAllMeals().isEmpty()) {
                    mealDb.mealDao().insertAll(
                            new Meal("breakfast"),
                            new Meal("lunch"),
                            new Meal("dinner"),
                            new Meal("snacks")
                    );
                }
            } catch (Exception e) {
                Log.e("DB_INIT", "Error initializing meals", e);
                runOnUiThread(() ->
                        Toast.makeText(this, "MAIN: Error initializing meals", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    //TODO: Delete later
    private void showMealsDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_layout);

        RecyclerView mealsRecyclerView = dialog.findViewById(R.id.mealsRecyclerView);
        mealsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        new Thread(() -> {
            final List<Meal>[] meals = new List[]{mealDb.mealDao().getAllMeals()};
            runOnUiThread(() -> {
                if (meals[0].isEmpty()) {
                    // Add default meals if empty
                    mealDb.mealDao().insertAll(
                            new Meal("breakfast"),
                            new Meal("lunch"),
                            new Meal("dinner"),
                            new Meal("snacks")
                    );
                    meals[0] = mealDb.mealDao().getAllMeals(); // Reload
                }
                MealAdapter adapter = new MealAdapter(meals[0]);
                mealsRecyclerView.setAdapter(adapter);
            });
        }).start();
        dialog.findViewById(R.id.btnCloseMeals).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void viewOptions(View v) {
        //TODO: Add Favorites, View Favorites, Delete Favorites, Add Frequents, View Frequents, Delete Frequents
        // Add Favorites: Load recipes from recipeList, on click selected recipe saved to DB
        // In loading recipes from recipeList, check for duplicates
        // View Favorites: Load recipes from DB, on click selected recipe sent to chat UI
        // Delete Favorites: Load recipes from DB, on click selected recipe deleted from DB
        // Add Frequents: Get input from user: (1) Title, (2) all ingredients, then format as below, and save to DB
        // e.g. "I have milk, sausage and eggs" -> "milk,sausage,eggs"
        // View Frequents: Load all saved frequents from DB, on click selected frequents loaded into user chatbox
        // e.g. "I have [x,y,z], what can I make?"
        // Delete Frequents: Load all saved frequents from DB, on click selected frequents deleted from DB
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.options_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            if (id == R.id.menu_add_favorites) {
                showRecipeSelectionDialog();
                return true;
            } else if (id == R.id.menu_view_favorites) {
                showFavoritesDialog();
                return true;
            } else if (id == R.id.view_meals) {
                showMealsDialog();
            } else if (id == R.id.menu_delete_favorites) {
                deleteFavoriteDialog();
            }
            return false;
        });

        popup.show();
    }

    private void deleteFavoriteDialog() {
        // Fetch all favorites
        List<Recipe> favorites = dbHelper.getAllFavoriteRecipes();

        if (favorites.isEmpty()) {
            Toast.makeText(this, "No favorites saved yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a list of titles for the AlertDialog
        List<String> titles = new ArrayList<>();
        for (Recipe recipe : favorites) {
            titles.add(recipe.getTitle());
        }

        String[] recipeArray = titles.toArray(new String[0]);

        // Display the list of recipes in a Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Your Favorite Recipes");
        builder.setItems(recipeArray, (dialog, which) -> {
            // On click, get the full recipe details
            Recipe selectedRecipe = favorites.get(which);

            // Show the full details in a new dialog
            boolean result = dbHelper.deleteFavorite(selectedRecipe);

            if (result) {
                Toast.makeText(this, "Favorite Deleted!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Error, delete failed!", Toast.LENGTH_LONG).show();
            }
        });
        builder.show();
    }


    public void addIngredientsToMeal(String mealName, List<String> newIngredients) {
        new Thread(() -> {
            try {
                Meal meal = mealDb.mealDao().getMealByName(mealName);
                if (meal == null) {
                    meal = new Meal(mealName);
                    mealDb.mealDao().insertAll(
                            new Meal("breakfast"),
                            new Meal("lunch"),
                            new Meal("dinner"),
                            new Meal("snacks")
                    );
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

    //TODO: Delete later
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

    //TODO: Delete later
    public void handleAddIngredients(String mealName, String ingredients) {
        List<String> ingredientList = Arrays.asList(ingredients.split(","));
        mealRepository.addIngredientsToMeal(mealName, ingredientList);

        // Debug verification
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            new Thread(() -> {
                Meal meal = MealDatabase.getDatabase(this)
                        .mealDao()
                        .getMealByName(mealName.toLowerCase());
                runOnUiThread(() -> {
                    if (meal != null) {
                        Log.d("MEAL_DEBUG", "Current ingredients: " + meal.getIngredients());
                        Toast.makeText(this,
                                "Updated " + mealName + ": " + meal.getIngredients(),
                                Toast.LENGTH_LONG).show();
                    }
                });
            }).start();
        }, 1000);
    }

    public void removeIngredientFromMeal(String mealName, String ingredient) {
        new Thread(() -> {
            try {
                Meal meal = mealDb.mealDao().getMealByName(mealName);
                if (meal != null) {
                    List<String> ingredients = new ArrayList<>(meal.getIngredients());
                    ingredients.remove(ingredient.toLowerCase());
                    meal.setIngredients(ingredients);
                    mealDb.mealDao().update(meal);

                    runOnUiThread(() ->
                            Toast.makeText(this, "Removed from " + mealName, Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e("REMOVE_ERROR", "Failed to remove ingredient", e);
                runOnUiThread(() ->
                        Toast.makeText(this, "Removal failed", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void showRecipeSelectionDialog() {
        // Pull the current recipes displayed in the chat
        List<String> searchResults = new ArrayList<>();
        for (Recipe recipe : recipeList) {
            searchResults.add(recipe.getTitle());
        }

        if (searchResults.isEmpty()) {
            Toast.makeText(this, "No recipes found. Search first!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert the list to an array to display in the dialog
        String[] recipeArray = searchResults.toArray(new String[0]);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select a Recipe to Add to Favorites");
        builder.setItems(recipeArray, (dialog, which) -> {
            // When an item is clicked, get the title from the list
            String selectedRecipe = recipeArray[which];

            // Find the recipe object that matches the title
            Recipe matchingRecipe = null;
            for (Recipe recipe : recipeList) {
                if (recipe.getTitle().equals(selectedRecipe)) {
                    matchingRecipe = recipe;
                    break;
                }
            }

            if (matchingRecipe != null) {
                // Save the complete recipe to the database
                dbHelper.addToFavorites(matchingRecipe.getTitle(),
                        "Ingredients:\n" + matchingRecipe.getIngredients() +
                                "\n\nInstructions:\n" + matchingRecipe.getInstructions());
                Toast.makeText(this, matchingRecipe.getTitle() + " added to favorites!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Recipe not found.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }



    private void showFavoritesDialog() {
        // Fetch all favorites
        List<Recipe> favorites = dbHelper.getAllFavoriteRecipes();

        if (favorites.isEmpty()) {
            Toast.makeText(this, "No favorites saved yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a list of titles for the AlertDialog
        List<String> titles = new ArrayList<>();
        for (Recipe recipe : favorites) {
            titles.add(recipe.getTitle());
        }

        String[] recipeArray = titles.toArray(new String[0]);

        // Display the list of recipes in a Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Your Favorite Recipes");
        builder.setItems(recipeArray, (dialog, which) -> {
            // On click, get the full recipe details
            Recipe selectedRecipe = favorites.get(which);

            // Show the full details in a new dialog
            showRecipeDetailsDialog(selectedRecipe);
        });
        builder.show();
    }

    private void showRecipeDetailsDialog(Recipe recipe) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(recipe.getTitle());

        // Format the details
        String details = "Ingredients:\n" + recipe.getIngredients() + "\n\n" +
                "Instructions:\n" + recipe.getInstructions();

        builder.setMessage(details);
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
        builder.show();
    }





}