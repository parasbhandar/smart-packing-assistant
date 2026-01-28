package com.example.packyourbag.Services;

import android.content.Context;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AIRecommendationService {
    private static final String GEMINI_API_KEY = "";
    private static final String GEMINI_URL = "";

    private final Context context;
    private final RequestQueue requestQueue;
    private final WeatherService weatherService;

    public AIRecommendationService(Context context) {
        this.context = context;
        this.requestQueue = Volley.newRequestQueue(context);
        this.weatherService = new WeatherService(context);
    }

    public interface RecommendationCallback {
        void onSuccess(RecommendationData recommendations);
        void onError(String error);
    }

    public static class RecommendationData {
        public List<String> essentialItems;
        public List<String> weatherSpecificItems;
        public List<String> activityBasedItems;
        public List<String> safetyItems;
        public String packingTips;
        public String weatherAlert;
        public List<String> notifications;
        public long timestamp;

        public RecommendationData() {
            this.essentialItems = new ArrayList<>();
            this.weatherSpecificItems = new ArrayList<>();
            this.activityBasedItems = new ArrayList<>();
            this.safetyItems = new ArrayList<>();
            this.notifications = new ArrayList<>();
            this.timestamp = System.currentTimeMillis();
        }
    }

    public void generateSmartRecommendations(String destination, int duration, String tripType,
                                             String startDate, String endDate,
                                             RecommendationCallback callback) {

        // Get weather first
        weatherService.getDetailedWeather(destination, new WeatherService.WeatherCallback() {
            @Override
            public void onSuccess(WeatherService.WeatherData weatherData) {
                generateAIRecommendations(destination, duration, tripType, startDate, endDate,
                        weatherData, callback);
            }

            @Override
            public void onError(String error) {
                generateAIRecommendations(destination, duration, tripType, startDate, endDate,
                        null, callback);
            }
        });
    }

    private void generateAIRecommendations(String destination, int duration, String tripType,
                                           String startDate, String endDate,
                                           WeatherService.WeatherData weatherData,
                                           RecommendationCallback callback) {

        String prompt = buildRecommendationPrompt(destination, duration, tripType,
                startDate, endDate, weatherData);

        // Call Gemini directly
        callGemini(prompt, callback);
    }

    private String buildRecommendationPrompt(String destination, int duration, String tripType,
                                             String startDate, String endDate,
                                             WeatherService.WeatherData weatherData) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("As a professional travel packing advisor, provide personalized packing recommendations for:\n\n");
        prompt.append("TRIP DETAILS:\n");
        prompt.append("- Destination: ").append(destination).append("\n");
        prompt.append("- Duration: ").append(duration).append(" days\n");
        prompt.append("- Trip Type: ").append(tripType).append("\n");
        prompt.append("- Dates: ").append(startDate).append(" to ").append(endDate).append("\n\n");

        if (weatherData != null) {
            prompt.append("CURRENT WEATHER CONDITIONS:\n");
            prompt.append("- Current Temperature: ").append(Math.round(weatherData.currentTemp)).append("°C\n");
            prompt.append("- Weather Condition: ").append(weatherData.currentDescription).append("\n");
            prompt.append("- Humidity: ").append(weatherData.humidity).append("%\n");
            prompt.append("- Wind Speed: ").append(weatherData.windSpeed).append(" m/s\n");
        }

        prompt.append("\nPlease provide recommendations in the following JSON format:\n");
        prompt.append("{\n");
        prompt.append("  \"essentialItems\": [\"item1\", \"item2\"],\n");
        prompt.append("  \"weatherSpecificItems\": [\"item1\", \"item2\"],\n");
        prompt.append("  \"activityBasedItems\": [\"item1\", \"item2\"],\n");
        prompt.append("  \"safetyItems\": [\"item1\", \"item2\"],\n");
        prompt.append("  \"packingTips\": \"Detailed packing tips\",\n");
        prompt.append("  \"weatherAlert\": \"Weather related alerts\",\n");
        prompt.append("  \"notifications\": [\"notification1\", \"notification2\"]\n");
        prompt.append("}\n");

        return prompt.toString();
    }

    private void callGemini(String prompt, RecommendationCallback callback) {
        try {
            JSONObject requestBody = new JSONObject();

            JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();

            JSONArray parts = new JSONArray();
            JSONObject part = new JSONObject();
            part.put("text", prompt);
            parts.put(part);

            content.put("parts", parts);
            contents.put(content);
            requestBody.put("contents", contents);

            JSONObject generationConfig = new JSONObject();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("maxOutputTokens", 1500);
            requestBody.put("generationConfig", generationConfig);

            String url = GEMINI_URL + "?key=" + GEMINI_API_KEY;

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, requestBody,
                    response -> {
                        try {
                            JSONArray candidates = response.getJSONArray("candidates");
                            JSONObject candidate = candidates.getJSONObject(0);
                            JSONArray responseParts = candidate.getJSONObject("content").getJSONArray("parts");
                            String responseText = responseParts.getJSONObject(0).getString("text");

                            RecommendationData recommendations = parseAIResponse(responseText);
                            callback.onSuccess(recommendations);
                        } catch (Exception e) {
                            generateFallbackRecommendations(callback);
                        }
                    },
                    error -> generateFallbackRecommendations(callback)
            ) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            requestQueue.add(request);

        } catch (Exception e) {
            generateFallbackRecommendations(callback);
        }
    }

    private RecommendationData parseAIResponse(String response) {
        RecommendationData data = new RecommendationData();

        try {
            int start = response.indexOf("{");
            int end = response.lastIndexOf("}") + 1;

            if (start >= 0 && end > start) {
                String jsonString = response.substring(start, end);
                JSONObject json = new JSONObject(jsonString);

                if (json.has("essentialItems")) {
                    data.essentialItems = parseStringArray(json.getJSONArray("essentialItems"));
                }
                if (json.has("weatherSpecificItems")) {
                    data.weatherSpecificItems = parseStringArray(json.getJSONArray("weatherSpecificItems"));
                }
                if (json.has("activityBasedItems")) {
                    data.activityBasedItems = parseStringArray(json.getJSONArray("activityBasedItems"));
                }
                if (json.has("safetyItems")) {
                    data.safetyItems = parseStringArray(json.getJSONArray("safetyItems"));
                }
                if (json.has("notifications")) {
                    data.notifications = parseStringArray(json.getJSONArray("notifications"));
                }

                data.packingTips = json.optString("packingTips", "");
                data.weatherAlert = json.optString("weatherAlert", "");
            }
        } catch (Exception ignored) {}

        return data;
    }

    private List<String> parseStringArray(JSONArray jsonArray) {
        List<String> list = new ArrayList<>();
        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                list.add(jsonArray.getString(i));
            }
        } catch (Exception ignored) {}
        return list;
    }

//    private void generateFallbackRecommendations(RecommendationCallback callback) {
//        RecommendationData data = new RecommendationData();
//
//        data.essentialItems.add("Passport/ID");
//        data.essentialItems.add("Phone charger");
//        data.essentialItems.add("Toothbrush");
//        data.essentialItems.add("Toothpaste");
//        data.essentialItems.add("Underwear");
//        data.essentialItems.add("Socks");
//
//        data.weatherSpecificItems.add("Check weather before departure");
//        data.weatherSpecificItems.add("Pack layers for temperature changes");
//
//        data.safetyItems.add("First aid kit");
//        data.safetyItems.add("Emergency contacts");
//
//        data.packingTips = "AI unavailable. Pack according to weather and trip type.";
//        data.notifications.add("Fallback recommendations applied.");
//
//        callback.onSuccess(data);
//    }
private void generateFallbackRecommendations(RecommendationCallback callback) {
    RecommendationData data = new RecommendationData();

    data.essentialItems.add("Passport/ID");
    data.essentialItems.add("Phone charger");
    data.essentialItems.add("Toothbrush");
    data.essentialItems.add("Toothpaste");
    data.essentialItems.add("Underwear");
    data.essentialItems.add("Socks");

    data.weatherSpecificItems.add("Check weather before departure");
    data.weatherSpecificItems.add("Pack layers for temperature changes");

    data.safetyItems.add("First aid kit");
    data.safetyItems.add("Emergency contacts");

    // Initialize the strings that might be null
    data.packingTips = "AI unavailable. Pack according to weather and trip type.";
    data.weatherAlert = ""; // Initialize as empty string instead of leaving null
    data.notifications.add("Fallback recommendations applied.");

    callback.onSuccess(data);
}
}