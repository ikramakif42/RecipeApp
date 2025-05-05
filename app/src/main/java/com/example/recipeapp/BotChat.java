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
    private ArrayList<JSONObject> conversationHistory;
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
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(API_URL).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            String userMessage = params[0];
            String classifierPrompt = "You are a task classifier and input corrector. Based on the user input, respond with a JSON object that includes:\n + " +
                    "1. intent: one of [get_recipes, load_preset, save_favorite, calorie_filter]\n" +
                    "2. value: cleaned data to use based on intent.\n\n" +
                    "Rules:\n" +
                    "- If intent is get_recipes: 'value' must be a comma-separated list of ingredients, corrected for typos.\n" +
                    "- If intent is load_preset: 'value' is the preset name (e.g., breakfast, lunch).\n" +
                    "- If intent is save_favorite: 'value' is the recipe name or ID if mentioned.\n" +
                    "- If intent is calorie_filter: 'value' is the calorie limit as a number (e.g., 500).\n\n" +
                    "Respond ONLY with a valid JSON object, NOTHING ELSE like:\n" +
                    "{ \"intent\": \"get_recipes\", \"value\": \"apples,flour,sugar\" }\n\n" +
                    "User input: \"" + userMessage + "\"";

            JSONObject requestBody = buildRequestBody(classifierPrompt);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
            }

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return "{\"error\":\"API request failed\"}";
            }

            String rawResponse = readResponse(connection.getInputStream());
            JSONObject json = new JSONObject(rawResponse);
            JSONObject candidate = json.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0);
            Log.d("response", candidate.toString(4));

            String textResponse = candidate.getString("text")
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```", "")
                    .trim();
            Log.d("Clean Response", textResponse);

            String intent = "";
            String value = "";
            try {
                JSONObject parsed = new JSONObject(textResponse);
                Log.d("parsed", parsed.toString(4));
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

            String result = "";

            switch (intent) {
                case "get_recipes":
                    Log.d("get_recipes", userMessage+"\n"+value);
                    result = SpoonHelper.getRecipesByIngredients(value)
                            .replaceAll("(?i)<ol>|</ol>", "")
                            .replaceAll("(?i)</li>", "")
                            .replaceAll("(?i)<li>", "• $1\n")
                            .replaceAll("<[^>]+>", "")
                            .trim();

                    JSONObject recipeResponse = new JSONObject();
                    JSONArray candidates = new JSONArray();
                    JSONObject cand = new JSONObject();
                    JSONObject content = new JSONObject();
                    JSONArray parts = new JSONArray();

                    parts.put(new JSONObject().put("text", result));
                    content.put("parts", parts);
                    cand.put("content", content);
                    candidates.put(cand);
                    recipeResponse.put("candidates", candidates);

                    return recipeResponse.toString();

                case "calorie_filter":
                    Log.d("calorie_filter", userMessage+"\n"+value);
                    result = "Recalculate the portion size from the previous messages such that the calories for each recipe are not more that "+value;
                    break;
                case "load_preset":
                    Log.d("load_preset", userMessage+"\n"+value);
//                    List<String> ingredients = SQLiteHelper.loadPreset(userMessage);  // you define
//                    result = SpoonacularHelper.getRecipesByIngredients(String.join(",", ingredients));
                    break;
                case "save_favorite":
                    Log.d("save_favorite", userMessage+"\n"+value);
//                    SQLiteHelper.saveFavoriteFromText(userMessage);  // you define
//                    result = "Recipe saved.";
                    break;
                default:
                    Log.d("default", userMessage+"\n"+value);
                    result = "Sorry, I couldn't understand your request. Please rephrase.";
//                    result = "Unrecognized task.";
                    break;
            }



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
            return "{\"error\":\"" + e.getMessage() + "\"}";
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private JSONObject buildRequestBody(String message) throws JSONException {
        JSONObject requestBody = new JSONObject();
        JSONArray contents = new JSONArray();

        for (JSONObject historyItem : conversationHistory) {
            contents.put(historyItem);
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

                JSONObject botResponse = new JSONObject();
                JSONObject content = new JSONObject();
                JSONArray parts = new JSONArray();
                parts.put(new JSONObject().put("text", responseText));
                content.put("parts", parts);
                content.put("role", "model");
                botResponse.put("content", content);
                conversationHistory.add(botResponse);

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