package com.example.recipeapp;

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

public class BotChat extends AsyncTask<String, Void, String> {
    private ApiResponseListener listener;
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + BuildConfig.GEMINI_API_KEY;
    public BotChat(ApiResponseListener listener) {
        this.listener = listener;
    }

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

            String classifierPrompt = "You are a task classifier and input corrector. Based on the user input, respond with a JSON object that includes:\n + " +
                    "1. intent: one of [get_recipes, save_preset, save_favorite, calorie_filter]\n" +
                    "2. value: cleaned data to use based on intent.\n\n" +
                    "Rules:\n" +
                    "- If intent is get_recipes: 'value' must be a comma-separated list of ingredients, corrected for typos.\n" +
                    "- If intent is save_preset: 'value' is the preset of comma-separated list of ingredients and the name (e.g., \"breakfast,lunch\").\n" +
                    "- If intent is save_favorite: 'value' is the recipe name or ID if mentioned.\n" +
                    "- If intent is calorie_filter: 'value' is the calorie limit as a number (e.g., 500).\n\n" +
                    "Respond ONLY with a valid JSON object, NOTHING ELSE like:\n" +
                    "{ \"intent\": \"get_recipes\", \"value\": \"apples,flour,sugar\" }\n\n" +
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
                case "save_preset":
                    Log.d("save_preset", userMessage+"\n"+value);
                    //TODO: SQLite Logic to save preset from chatbot
//                    List<String> ingredients = SQLiteHelper.savePreset(userMessage);
//                    result = SpoonacularHelper.getRecipesByIngredients(String.join(",", ingredients));
                    break;
                case "save_favorite":
                    Log.d("save_favorite", userMessage+"\n"+value);
                    //TODO: SQLite Logic to save favorite recipe from chatbot
//                    SQLiteHelper.saveFavoriteFromText(userMessage);  // you define
//                    result = "Recipe saved.";
                    break;
                default:
                    Log.d("default", userMessage+"\n"+value);
                    result = "Sorry, I couldn't understand your request. Please rephrase.";
//                    result = "Unrecognized task.";
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

    public interface ApiResponseListener {
        void onApiResponse(String response);
    }
}