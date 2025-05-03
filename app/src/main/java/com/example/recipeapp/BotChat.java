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

public class BotChat extends AsyncTask<String, Void, String> {
    private ApiResponseListener listener;
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + BuildConfig.API_KEY;

    public BotChat(ApiResponseListener listener) {
        this.listener = listener;
    }

    @Override
    protected String doInBackground(String... params) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(API_URL).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            JSONObject requestBody = buildRequestBody(params[0]);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
            }

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                return readResponse(connection.getInputStream());
            } else {
                return "{\"error\":\"API request failed\"}";
            }
        } catch (Exception e) {
            return "{\"error\":\"" + e.getMessage() + "\"}";
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private JSONObject buildRequestBody(String message) throws JSONException {
        JSONObject requestBody = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject content = new JSONObject();
        JSONArray parts = new JSONArray();

        parts.put(new JSONObject().put("text", message));
        content.put("role", "user");
        content.put("parts", parts);
        contents.put(content);

        return requestBody.put("contents", contents);
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