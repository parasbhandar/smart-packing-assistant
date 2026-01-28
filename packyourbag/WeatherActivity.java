package com.example.packyourbag;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.packyourbag.Adapter.HourlyForecastAdapter;
import com.example.packyourbag.Services.WeatherService;
import com.example.packyourbag.Services.AIRecommendationService;

public class WeatherActivity extends AppCompatActivity {
    private TextView textWeatherInfo, textSuggestions, textTripDates, textLastUpdated;
    private RecyclerView recyclerHourlyForecast;
    private LinearLayout layoutDailyForecast;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private Button btnRefresh, btnGetAIRecommendations;

    private String destination, startDate, endDate, tripType;
    private int duration;

    private WeatherService weatherService;
    private AIRecommendationService aiService;

    private Handler autoRefreshHandler;
    private Runnable autoRefreshRunnable;
    private static final long AUTO_REFRESH_INTERVAL = 60000; // 1 minute


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enhanced_weather);

        destination = getIntent().getStringExtra("destination");
        startDate = getIntent().getStringExtra("startDate");
        endDate = getIntent().getStringExtra("endDate");
        tripType = getIntent().getStringExtra("tripType");
        duration = getIntent().getIntExtra("duration", 1);

        initViews();
        initServices();
        setupClickListeners(); // ADD THIS LINE
        setupAutoRefresh();
        loadWeatherData();
    }

    private void initViews() {
        textWeatherInfo = findViewById(R.id.textWeatherInfo);
        textSuggestions = findViewById(R.id.textSuggestions);
        textTripDates = findViewById(R.id.textTripDates);
        textLastUpdated = findViewById(R.id.textLastUpdated);
        recyclerHourlyForecast = findViewById(R.id.recyclerHourlyForecast);
        layoutDailyForecast = findViewById(R.id.layoutDailyForecast);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        progressBar = findViewById(R.id.progressBar);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnGetAIRecommendations = findViewById(R.id.btnGetAIRecommendations);

        if (destination != null) {
            setTitle("Weather - " + destination);
        }

        if (startDate != null && endDate != null) {
            textTripDates.setText("Trip Duration: " + startDate + " to " + endDate + " (" + duration + " days)");
        }

        // Setup hourly forecast RecyclerView
        recyclerHourlyForecast.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    }

    private void initServices() {
        weatherService = new WeatherService(this);
        aiService = new AIRecommendationService(this);
    }

    // ADD THIS METHOD - This was missing!
    private void setupClickListeners() {
        // AI Recommendations button click listener
        btnGetAIRecommendations.setOnClickListener(v -> {
            generateAIRecommendations();
        });

        // Refresh button click listener
        btnRefresh.setOnClickListener(v -> {
            loadWeatherData();
        });

        // Swipe to refresh
        swipeRefresh.setOnRefreshListener(() -> {
            loadWeatherData();
        });
    }

    private void setupAutoRefresh() {
        autoRefreshHandler = new Handler();
        autoRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                loadWeatherData();
                autoRefreshHandler.postDelayed(this, AUTO_REFRESH_INTERVAL);
            }
        };
    }

    private void loadWeatherData() {
        if (destination == null) return;

        progressBar.setVisibility(View.VISIBLE);
        swipeRefresh.setRefreshing(true);

        weatherService.getDetailedWeather(destination, new WeatherService.WeatherCallback() {
            @Override
            public void onSuccess(WeatherService.WeatherData weatherData) {
                runOnUiThread(() -> {
                    displayWeatherInfo(weatherData);
                    displayHourlyForecast(weatherData);
                    displayDailyForecast(weatherData);
                    updateLastRefreshTime();
                    progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    textWeatherInfo.setText("Failed to load weather data: " + error);
                    showGenericSuggestions();
                    progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(WeatherActivity.this, "Weather update failed: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void displayWeatherInfo(WeatherService.WeatherData weatherData) {
        String weatherInfo = weatherService.formatWeatherSummary(weatherData);
        textWeatherInfo.setText(weatherInfo);

        // Update title with current temperature
        setTitle("Weather - " + weatherData.cityName + " (" + Math.round(weatherData.currentTemp) + "°C)");
    }

    private void displayHourlyForecast(WeatherService.WeatherData weatherData) {
        if (!weatherData.hourlyForecasts.isEmpty()) {
            HourlyForecastAdapter adapter = new HourlyForecastAdapter(weatherData.hourlyForecasts);
            recyclerHourlyForecast.setAdapter(adapter);
        }
    }

    private void displayDailyForecast(WeatherService.WeatherData weatherData) {
        layoutDailyForecast.removeAllViews();

        if (!weatherData.dailyForecasts.isEmpty()) {
            TextView headerDaily = new TextView(this);
            headerDaily.setText("Daily Forecast:");
            headerDaily.setTextSize(16);
            headerDaily.setTypeface(null, android.graphics.Typeface.BOLD);
            headerDaily.setPadding(0, 16, 0, 8);
            layoutDailyForecast.addView(headerDaily);

            for (WeatherService.DailyForecast forecast : weatherData.dailyForecasts) {
                TextView dayView = new TextView(this);
                String dayText = forecast.getFormattedDate() + ": " +
                        Math.round(forecast.minTemp) + "-" + Math.round(forecast.maxTemp) + "°C, " +
                        forecast.description;
                if (forecast.precipitationChance > 0) {
                    dayText += " (" + Math.round(forecast.precipitationChance) + "% rain)";
                }
                dayView.setText(dayText);
                dayView.setPadding(16, 4, 0, 4);
                layoutDailyForecast.addView(dayView);
            }
        }
    }

    private void generateAIRecommendations() {
        if (destination == null) {
            Toast.makeText(this, "Destination not available", Toast.LENGTH_SHORT).show();
            return;
        }



        // Add debug logging
        android.util.Log.d("WeatherActivity", "Starting AI recommendations generation for: " + destination);

        progressBar.setVisibility(View.VISIBLE);
        btnGetAIRecommendations.setEnabled(false);
        btnGetAIRecommendations.setText("Generating AI Recommendations...");

        aiService.generateSmartRecommendations(destination, duration, tripType, startDate, endDate,
                new AIRecommendationService.RecommendationCallback() {
                    @Override
                    public void onSuccess(AIRecommendationService.RecommendationData recommendations) {
                        android.util.Log.d("WeatherActivity", "AI recommendations success");
                        runOnUiThread(() -> {
                            displayAIRecommendations(recommendations);
                            progressBar.setVisibility(View.GONE);
                            btnGetAIRecommendations.setEnabled(true);
                            btnGetAIRecommendations.setText("Refresh AI Recommendations");
                        });
                    }

                    @Override
                    public void onError(String error) {
                        android.util.Log.e("WeatherActivity", "AI recommendations error: " + error);
                        runOnUiThread(() -> {
                            showGenericSuggestions();
                            progressBar.setVisibility(View.GONE);
                            btnGetAIRecommendations.setEnabled(true);
                            btnGetAIRecommendations.setText("Get AI Recommendations");
                            Toast.makeText(WeatherActivity.this, "AI recommendations failed: " + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void displayAIRecommendations(AIRecommendationService.RecommendationData recommendations) {
        StringBuilder suggestions = new StringBuilder();

        // Weather Alert (if any)
        if (!recommendations.weatherAlert.isEmpty()) {
            suggestions.append("🚨 WEATHER ALERT:\n").append(recommendations.weatherAlert).append("\n\n");
        }

        // Essential Items
        if (!recommendations.essentialItems.isEmpty()) {
            suggestions.append("✈️ ESSENTIAL ITEMS:\n");
            for (String item : recommendations.essentialItems) {
                suggestions.append("• ").append(item).append("\n");
            }
            suggestions.append("\n");
        }

        // Weather-Specific Items
        if (!recommendations.weatherSpecificItems.isEmpty()) {
            suggestions.append("🌦️ WEATHER-SPECIFIC ITEMS:\n");
            for (String item : recommendations.weatherSpecificItems) {
                suggestions.append("• ").append(item).append("\n");
            }
            suggestions.append("\n");
        }

        // Activity-Based Items
        if (!recommendations.activityBasedItems.isEmpty()) {
            suggestions.append("🎯 ACTIVITY-BASED ITEMS:\n");
            for (String item : recommendations.activityBasedItems) {
                suggestions.append("• ").append(item).append("\n");
            }
            suggestions.append("\n");
        }

        // Safety Items
        if (!recommendations.safetyItems.isEmpty()) {
            suggestions.append("🛡️ SAFETY ITEMS:\n");
            for (String item : recommendations.safetyItems) {
                suggestions.append("• ").append(item).append("\n");
            }
            suggestions.append("\n");
        }

        // Packing Tips
        if (!recommendations.packingTips.isEmpty()) {
            suggestions.append("💡 PACKING TIPS:\n").append(recommendations.packingTips).append("\n\n");
        }

        // Show notifications if any
        if (!recommendations.notifications.isEmpty()) {
            for (String notification : recommendations.notifications) {
                Toast.makeText(this, notification, Toast.LENGTH_LONG).show();
            }
        }

        textSuggestions.setText(suggestions.toString());
    }

    private void showGenericSuggestions() {
        textSuggestions.setText("Generic packing suggestions:\n\n" +
                "• Check local weather before departure\n" +
                "• Pack layers for temperature changes\n" +
                "• Bring comfortable walking shoes\n" +
                "• Pack appropriate clothing for activities\n" +
                "• Don't forget rain protection\n" +
                "• Consider local customs and dress codes\n" +
                "• Pack essential medications and documents\n");
    }

    private void updateLastRefreshTime() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault());
        String currentTime = sdf.format(new java.util.Date());
        textLastUpdated.setText("Last updated: " + currentTime);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Start auto-refresh
        autoRefreshHandler.postDelayed(autoRefreshRunnable, AUTO_REFRESH_INTERVAL);
        // Load fresh data when resuming
        loadWeatherData();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop auto-refresh to save battery
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (autoRefreshHandler != null) {
            autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
        }
    }
}