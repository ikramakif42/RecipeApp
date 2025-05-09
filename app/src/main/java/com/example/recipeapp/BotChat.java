package com.example.recipeapp;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BotChat extends AsyncTask<String, Void, String> {
    private ApiResponseListener listener;
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + BuildConfig.GEMINI_API_KEY;
    public BotChat(ApiResponseListener listener) {
        this.listener = listener;
    }
    private MealDatabase mealDb;
    private List<Meal> meals = new ArrayList<>();

    private String readResponse(InputStream inputStream) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        return response.toString();
    }

    @Override
    protected String doInBackground(String... params) {
        Log.d("HISTORY", MainActivity.conversationHistory.toString());
        Log.d("RECIPE_LIST", MainActivity.recipeList.toString());
        HttpURLConnection connection = null;
        try {
            // Setup connection
            connection = (HttpURLConnection) new URL(API_URL).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Save history and prompt Gemini
            String userMessage = params[0];
            JSONObject userMessageObj = new JSONObject();
            JSONArray parts = new JSONArray();
            parts.put(new JSONObject().put("text", userMessage));
            userMessageObj.put("role", "user");
            userMessageObj.put("parts", parts);
            MainActivity.conversationHistory.add(userMessageObj);

//            String classifierPrompt = "You are a task classifier and input corrector. Based on the user input, respond with a JSON object that includes:\n + " +
//                    "1. intent: one of [get_recipes, save_preset, save_favorite, calorie_filter]\n" +
//                    "2. value: cleaned data to use based on intent.\n\n" +
//                    "Rules for meals:\n" +
//                    "- If intent is save_meal: 'value' must be a JSON string with 'meal' (breakfast/lunch/dinner/snacks) and 'ingredients' (comma-separated list)\n" +
//                    "- If intent is use_meal: 'value' must be the meal name (breakfast/lunch/dinner/snacks)\n\n" +
//                    "Example responses:\n" +
//                    "User: 'add eggs and bacon to breakfast'\n" +
//                    "Response: {\"intent\":\"save_meal\",\"value\":\"{\\\"meal\\\":\\\"breakfast\\\",\\\"ingredients\\\":\\\"eggs,bacon\\\"}\"}\n\n" +
//                    "Rules:\n" +
//                    "- If intent is get_recipes: 'value' must be a comma-separated list of ingredients, corrected for typos.\n" +
//                    "- If intent is calorie_filter: 'value' is the calorie limit as a number (e.g., 500).\n\n" +
//                    "Respond ONLY with a valid JSON object, NOTHING ELSE like:\n" +
//                    "{ \"intent\": \"get_recipes\", \"value\": \"apples,flour,sugar\" }\n\n" +
//                    "User input: \"" + userMessage + "\"";
//            String classifierPrompt = "You are a task classifier. Classify user input into one of these intents:\n" +
//                    "1. get_recipes - when user asks for recipe suggestions\n" +
//                    "2. save_meal - when user wants to add ingredients to a meal category (breakfast/lunch/dinner/snacks)\n" +
//                    "3. use_meal - when user wants recipes for a specific meal category\n" +
//                    "4. calorie_filter - when user mentions calories\n\n" +
//
//                    "For save_meal intent, extract:\n" +
//                    "- meal: one of [breakfast, lunch, dinner, snacks]\n" +
//                    "- ingredients: comma-separated list\n\n" +
//
//                    "Example 1:\n" +
//                    "User: 'add eggs and bacon to breakfast'\n" +
//                    "Response: {\"intent\":\"save_meal\",\"value\":\"{\\\"meal\\\":\\\"breakfast\\\",\\\"ingredients\\\":\\\"eggs,bacon\\\"}\"}\n\n" +
//
//                    "Example 2:\n" +
//                    "User: 'what can I make for dinner?'\n" +
//                    "Response: {\"intent\":\"use_meal\",\"value\":\"dinner\"}\n\n" +
//
//                    "Current user input: \"" + userMessage + "\"";
            String classifierPrompt = "Classify user input into these intents:\n" +
                    "1. get_recipes - when asking for recipes\n" +
                    "2. save_meal - when adding to meal categories (format: {\"meal\":\"category\",\"ingredients\":\"item1,item2\"})\n" +
                    "3. use_meal - when requesting recipes for a category (format: \"category\")\n" +
                    "4. remove_ingredient - when removing items (format: {\"meal\":\"category\",\"ingredient\":\"item\"})\n\n" +

                    "Examples:\n" +
                    "User: 'cook using my breakfast items'\n" +
                    "Response: {\"intent\":\"use_meal\",\"value\":\"breakfast\"}\n\n" +

                    "User: 'remove eggs from breakfast'\n" +
                    "Response: {\"intent\":\"remove_ingredient\",\"value\":\"{\\\"meal\\\":\\\"breakfast\\\",\\\"ingredient\\\":\\\"eggs\\\"}\"}\n\n" +

                    "User input: \"" + userMessage + "\"";

            JSONObject requestBody = buildRequestBody(classifierPrompt);
            Log.d("API Request", requestBody.toString(4));

            // Get response
            try (OutputStream os = connection.getOutputStream()) {
                os.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
            }

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return "{\"error\":\"API request failed\"}";
            }

            // Extract response JSON
            String rawResponse = readResponse(connection.getInputStream());
            Log.d("API Response", rawResponse);
            JSONObject json = new JSONObject(rawResponse);
            JSONObject candidate = json.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0);
            Log.d("1st response", candidate.toString(4));

            String textResponse = candidate.getString("text")
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```", "")
                    .trim();
            Log.d("Clean Response", textResponse);

            // parse into JSON as needed
            String intent = "";
            String value = "";
            try {
                JSONObject parsed = new JSONObject(textResponse);
                Log.d("parsedJSON", parsed.toString(4));
                intent = parsed.optString("intent", "");
                value = parsed.optString("value", "");

                if (intent.isEmpty() || value.isEmpty()) {
                    throw new JSONException("Missing intent or value");
                }
            } catch (JSONException e) {
                Log.e("GeminiParse", "Failed to parse Gemini response: " + textResponse);
                intent = "unknown";
                value = "";
            }

            // Use cases
            String result = "";
            switch (intent) {
                case "get_recipes":
                    Log.d("get_recipes", userMessage+"\n"+value);
                    // Fix broken HTML tags
                    result = SpoonHelper.getRecipesByIngredients(value)
                            .replaceAll("(?i)<ol>|</ol>", "")
                            .replaceAll("(?i)</li>", "")
                            .replaceAll("(?i)<li>", "\n• ")
                            .replaceAll("<[^>]+>", "")
                            .trim();
                    Log.d("get_recipes", result);

                    // Update history
                    JSONObject botResponse = new JSONObject();
                    parts = new JSONArray();
                    parts.put(new JSONObject().put("text", result));
                    botResponse.put("role", "model");
                    botResponse.put("parts", parts);
                    MainActivity.conversationHistory.add(botResponse);

                    // Format into Gemini JSON structure
                    JSONObject recipeResponse = new JSONObject();
                    JSONArray candidates = new JSONArray();
                    JSONObject cand = new JSONObject();
                    JSONObject content = new JSONObject();
                    parts = new JSONArray();

                    parts.put(new JSONObject().put("text", result));
                    content.put("parts", parts);
                    cand.put("content", content);
                    candidates.put(cand);
                    recipeResponse.put("candidates", candidates);

                    return recipeResponse.toString();

                case "calorie_filter":
                    Log.d("calorie_filter", userMessage+"\n"+value);
                    String caloriePrompt = "Recalculate the portion size from the recipes in our conversation such that the calories" +
                            "for each recipe are not more than " + value +
                            ". Remove all markdown formatting, such as #. Respond in plaintext and keep it short and concise.";

                    HttpURLConnection calorieConn = (HttpURLConnection) new URL(API_URL).openConnection();
                    calorieConn.setRequestMethod("POST");
                    calorieConn.setRequestProperty("Content-Type", "application/json");
                    calorieConn.setDoOutput(true);

                    JSONObject calorieRequest = buildRequestBody(caloriePrompt);
                    try (OutputStream os = calorieConn.getOutputStream()) {
                        os.write(calorieRequest.toString().getBytes(StandardCharsets.UTF_8));
                    }

                    if (calorieConn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        return "{\"error\":\"Calorie filter request failed\"}";
                    }

                    return readResponse(calorieConn.getInputStream());

                case "save_meal":
                    try {
                        JSONObject mealData = new JSONObject(value);
                        String mealName = mealData.getString("meal").toLowerCase();
                        String ingredientsStr = mealData.getString("ingredients");

                        List<String> ingredients = new ArrayList<>();
                        for (String item : ingredientsStr.split(",")) {
                            String clean = item.trim().toLowerCase();
                            if (!clean.isEmpty()) ingredients.add(clean);
                        }

                        if (!ingredients.isEmpty()) {
                            Activity activity = (Activity) listener;
                            if (activity instanceof MainActivity) {
                                ((MainActivity)activity).addIngredientsToMeal(mealName, ingredients);
                            }
                            result = "✓ Added to " + mealName + ": " + String.join(", ", ingredients);
                        } else {
                            result = "No valid ingredients provided";
                        }
                    } catch (Exception e) {
                        result = "Error processing ingredients";
                        Log.e("MEAL_PARSE", "Failed to parse: " + value, e);
                    }
                    break;


                // In the switch(intent) block:
                case "use_meal":
                    try {
                        String mealName = value.toLowerCase();
                        Meal meal = mealDb.mealDao().getMealByName(mealName);

                        if (meal != null && !meal.getIngredients().isEmpty()) {
                            String ingredients = String.join(", ", meal.getIngredients());
                            result = SpoonHelper.getRecipesByIngredients(ingredients)
                                    .replaceAll("(?i)<[^>]+>", "") // Clean HTML tags
                                    .trim();
                        } else {
                            result = "Your " + mealName + " category is empty. Add ingredients first!";
                        }
                    } catch (Exception e) {
                        result = "Error accessing " + value + " category";
                        Log.e("MEAL_USE", "Error using meal", e);
                    }
                    break;

                case "remove_ingredient":
                    try {
                        JSONObject removalData = new JSONObject(value);
                        String mealName = removalData.getString("meal").toLowerCase();
                        String ingredientToRemove = removalData.getString("ingredient").toLowerCase().trim();

                        Activity activity = (Activity) listener;
                        if (activity instanceof MainActivity) {
                            ((MainActivity)activity).removeIngredientFromMeal(mealName, ingredientToRemove);
                            result = "✓ Removed " + ingredientToRemove + " from " + mealName;
                        }
                    } catch (Exception e) {
                        result = "Failed to remove ingredient";
                        Log.e("REMOVE_INGREDIENT", "Error parsing removal request", e);
                    }
                    break;
                default:
                    Log.d("default", userMessage+"\n"+value);
                    result = "Sorry, I couldn't understand your request. Please rephrase.";
                    break;
            }

            // Second response from Gemini to chat if needed
            HttpURLConnection formatConn = (HttpURLConnection) new URL(API_URL).openConnection();
            formatConn.setRequestMethod("POST");
            formatConn.setRequestProperty("Content-Type", "application/json");
            formatConn.setDoOutput(true);

            String formatterPrompt = "User asked: \"" + userMessage + "\"\n"
                    + "App responded with: \"" + result + "\"\n"
                    + "Please return a clear and friendly message to the user.";
            JSONObject formatRequest = buildRequestBody(formatterPrompt);
            try (OutputStream os = formatConn.getOutputStream()) {
                os.write(formatRequest.toString().getBytes(StandardCharsets.UTF_8));
            }

            if (formatConn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return "{\"error\":\"Formatting request failed\"}";
            }

            return readResponse(formatConn.getInputStream());
        } catch (Exception e) {
            Log.e("ERROR", e.toString());
            return "{\"error\":\"" + e.getMessage() + "\"}";
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private JSONObject buildRequestBody(String message) throws JSONException {
        JSONObject requestBody = new JSONObject();
        JSONArray contents = new JSONArray();

        if (!MainActivity.conversationHistory.isEmpty()) {
            for (JSONObject pastJSON : MainActivity.conversationHistory) {
                contents.put(pastJSON);
            }
        }

        JSONObject content = new JSONObject();
        JSONArray parts = new JSONArray();
        parts.put(new JSONObject().put("text", message));
        content.put("role", "user");
        content.put("parts", parts);
        contents.put(content);

        return requestBody.put("contents", contents);
    }

    @Override
    protected void onPostExecute(String response) {
        try {
            JSONObject json = new JSONObject(response);
            String responseText = "";

            if (json.has("candidates")) {
                JSONObject candidate = json.getJSONArray("candidates").getJSONObject(0);
                responseText = candidate.getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");

            } else if (json.has("error")) {
                responseText = "Error: " + json.getString("error");
            }

            if (listener != null) {
                listener.onApiResponse(responseText);
            }
        } catch (JSONException e) {
            if (listener != null) {
                listener.onApiResponse("Error parsing response");
            }
        }
    }

}