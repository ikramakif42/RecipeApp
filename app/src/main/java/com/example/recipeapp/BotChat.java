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
    private static final String TAG = "BotChat";
    private static final String API_KEY = BuildConfig.API_KEY;
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + API_KEY;

    public interface ApiResponseListener {
        void onApiResponse(String response);
    }

    public BotChat(ApiResponseListener listener) {
        this.listener = listener;
    }

    @Override
    protected String doInBackground(String... params) {
        String conversationHistory = params[0];
        String result = "";
        URL link;
        HttpURLConnection myConn = null;

        try {
            link = new URL(API_URL);
            myConn = (HttpURLConnection) link.openConnection();
            myConn.setRequestMethod("POST");
            myConn.setRequestProperty("Content-Type", "application/json");
            myConn.setDoOutput(true);
            JSONObject requestBody = new JSONObject();

            try {
                JSONArray contents = new JSONArray(conversationHistory);
                requestBody.put("contents", contents);
            } catch (JSONException e) {
                JSONArray contents = new JSONArray();
                JSONObject content = new JSONObject();
                JSONArray parts = new JSONArray();

                JSONObject textPart = new JSONObject();
                textPart.put("text", conversationHistory);
                parts.put(textPart);

                content.put("role", "user");
                content.put("parts", parts);
                contents.put(content);

                requestBody.put("contents", contents);
            }

            try (OutputStream os = myConn.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = myConn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream in = myConn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();
                result = response.toString();
            } else {
                Log.e(TAG, "HTTP error code: " + responseCode);
                result = "{\"error\": \"API request failed with code " + responseCode + "\"}";
            }

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            try {
                result = "{\"error\": \"" + e.getMessage() + "\"}";
            } catch (Exception ex) {
                result = "{\"error\": \"Unknown error occurred\"}";
            }
        } finally {
            if (myConn != null) {
                myConn.disconnect();
            }
        }
        return result;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);

        try {
            JSONObject myObject = new JSONObject(s);
            Log.d("json", myObject.toString(4));

            // Extract the actual response text
            String responseText = "";

            if (myObject.has("candidates")) {
                JSONArray candidates = myObject.getJSONArray("candidates");
                if (candidates.length() > 0) {
                    JSONObject candidate = candidates.getJSONObject(0);
                    JSONObject content = candidate.getJSONObject("content");
                    JSONArray parts = content.getJSONArray("parts");
                    if (parts.length() > 0) {
                        responseText = parts.getJSONObject(0).getString("text");
                    }
                }
            } else if (myObject.has("error")) {
                responseText = "Error: " + myObject.getString("error");
            }

            if (listener != null) {
                listener.onApiResponse(responseText);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            if (listener != null) {
                listener.onApiResponse("Error parsing response: " + e.getMessage());
            }
        }
    }
}