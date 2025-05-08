package com.example.recipeapp;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;

public class SpoonHelper {

    private static final String SPOONACULAR_API_KEY = BuildConfig.SPOON_API_KEY;

    public static String getRecipesByIngredients(String userIngredients) {
        HttpURLConnection connection = null;
        StringBuilder output = new StringBuilder();

        try {
            String encodedIngredients = URLEncoder.encode(userIngredients, StandardCharsets.UTF_8.name());

            String urlStr = "https://api.spoonacular.com/recipes/findByIngredients" +
                    "?ingredients=" + encodedIngredients +
                    "&number=3" +
                    "&ranking=1" +
                    "&ignorePantry=true" +
                    "&apiKey=" + SPOONACULAR_API_KEY;

            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return "Spoonacular API error: " + connection.getResponseCode();
            }

            String jsonResponse = readStream(connection.getInputStream());
            JSONArray results = new JSONArray(jsonResponse);

            if (results.length() == 0) return "No recipes found.";

            for (int i = 0; i < results.length(); i++) {
                JSONObject recipe = results.getJSONObject(i);
                int id = recipe.getInt("id");

                String recipeInfo = fetchFullRecipeById(id);
                output.append(recipeInfo).append("\n\n");
            }
            Log.d("Spoon API", output.toString());
            return output.toString().trim();

        } catch (Exception e) {
            Log.e("Spoon API", "Error fetching full recipes", e);
            return "Error: " + e.getMessage();
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String fetchFullRecipeById(int id) throws Exception {
        String infoUrl = "https://api.spoonacular.com/recipes/" + id + "/information" +
                "?includeNutrition=true&apiKey=" + SPOONACULAR_API_KEY; // Added includeNutrition=true

        HttpURLConnection conn = (HttpURLConnection) new URL(infoUrl).openConnection();
        conn.setRequestMethod("GET");

        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            return "Failed to get full recipe info for ID " + id;
        }

        String jsonResponse = readStream(conn.getInputStream());
        JSONObject data = new JSONObject(jsonResponse);

        String title = data.optString("title", "Untitled");
        // Remove the summary section completely
        String instructions = data.optString("instructions", "No instructions provided.");

        // Get calories if nutrition info is available
        String calorieInfo = "";
        if (data.has("nutrition") && !data.isNull("nutrition")) {
            JSONObject nutrition = data.getJSONObject("nutrition");
            if (nutrition.has("nutrients")) {
                JSONArray nutrients = nutrition.getJSONArray("nutrients");
                for (int i = 0; i < nutrients.length(); i++) {
                    JSONObject nutrient = nutrients.getJSONObject(i);
                    if ("Calories".equals(nutrient.optString("name"))) {
                        double calories = nutrient.optDouble("amount");
                        String unit = nutrient.optString("unit");
                        calorieInfo = "Calories: " + calories + " " + unit + "\n";
                        break;
                    }
                }
            }
        }

        // Get servings information
        String servingInfo = "";
        int servings = data.optInt("servings", 0);
        if (servings > 0) {
            servingInfo = "Servings: " + servings + "\n";
        }

        JSONArray ingredients = data.optJSONArray("extendedIngredients");
        StringBuilder ingredientList = new StringBuilder();
        if (ingredients != null) {
            for (int j = 0; j < ingredients.length(); j++) {
                JSONObject ing = ingredients.getJSONObject(j);
                ingredientList.append(j+1).append(". ")
                        .append(ing.optString("original", ""))
                        .append("\n");
            }
        }

        MainActivity.recipeList.add(
                new Recipe(String.valueOf(id), title, calorieInfo, servingInfo, ingredientList.toString(), instructions)
        );

        return title + "\n\n" +
                calorieInfo +
                servingInfo +
                "Ingredients:\n" + ingredientList.toString() + "\n" +
                "Instructions:\n" + instructions;
    }

    private static String readStream(InputStream inputStream) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        return response.toString();
    }
}
