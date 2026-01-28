package com.example.packyourbag.Services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.example.packyourbag.MainActivity;
import com.example.packyourbag.WeatherActivity;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EnhancedNotificationService {
    private static final String WEATHER_CHANNEL_ID = "weather_updates";
    private static final String RECOMMENDATIONS_CHANNEL_ID = "ai_recommendations";
    private static final String PACKING_CHANNEL_ID = "packing_reminders";

    private static final int WEATHER_NOTIFICATION_ID = 1001;
    private static final int RECOMMENDATION_NOTIFICATION_ID = 1002;
    private static final int PACKING_NOTIFICATION_ID = 1003;

    private Context context;
    private WeatherService weatherService;
    private AIRecommendationService aiService;
    private ScheduledExecutorService scheduler;

    public EnhancedNotificationService(Context context) {
        this.context = context;
        this.weatherService = new WeatherService(context);
        this.aiService = new AIRecommendationService(context);
        this.scheduler = Executors.newScheduledThreadPool(2);
        createNotificationChannels();
    }

    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

            // Weather Updates Channel
            NotificationChannel weatherChannel = new NotificationChannel(
                    WEATHER_CHANNEL_ID,
                    "Weather Updates",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            weatherChannel.setDescription("Real-time weather updates and alerts for your trips");
            notificationManager.createNotificationChannel(weatherChannel);

            // AI Recommendations Channel
            NotificationChannel aiChannel = new NotificationChannel(
                    RECOMMENDATIONS_CHANNEL_ID,
                    "AI Recommendations",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            aiChannel.setDescription("AI-powered packing recommendations based on weather changes");
            notificationManager.createNotificationChannel(aiChannel);

            // Packing Reminders Channel (existing)
            NotificationChannel packingChannel = new NotificationChannel(
                    PACKING_CHANNEL_ID,
                    "Packing Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            packingChannel.setDescription("Reminders for incomplete packing items");
            notificationManager.createNotificationChannel(packingChannel);
        }
    }

    private void createNotificationChannels() {
        createNotificationChannels(context);
    }

    /**
     * Check if notification permission is granted
     */
    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(context,
                    android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // For Android 12 and below, permission is granted by default
    }

    /**
     * Safe notification method that checks permissions before showing notifications
     */
    private void showNotification(NotificationCompat.Builder builder, int notificationId) {
        if (!hasNotificationPermission()) {
            // Log the issue but don't crash the app
            android.util.Log.w("EnhancedNotificationService",
                    "POST_NOTIFICATIONS permission not granted. Cannot show notification.");
            return;
        }

        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(notificationId, builder.build());
        } catch (SecurityException e) {
            android.util.Log.e("EnhancedNotificationService",
                    "SecurityException when showing notification: " + e.getMessage());
        }
    }

    // Weather Alert Notifications
    public void showWeatherAlert(String destination, String alertMessage, String tripId) {
        Intent intent = new Intent(context, WeatherActivity.class);
        intent.putExtra("destination", destination);
        intent.putExtra("tripId", tripId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, WEATHER_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Weather Alert - " + destination)
                .setContentText(alertMessage)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(alertMessage))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        showNotification(builder, WEATHER_NOTIFICATION_ID);
    }

    // AI Recommendation Updates
    public void showRecommendationUpdate(String destination, String updateMessage, String tripId) {
        Intent intent = new Intent(context, WeatherActivity.class);
        intent.putExtra("destination", destination);
        intent.putExtra("tripId", tripId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, RECOMMENDATIONS_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("New Packing Recommendations - " + destination)
                .setContentText(updateMessage)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(updateMessage))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        showNotification(builder, RECOMMENDATION_NOTIFICATION_ID);
    }

    // Enhanced packing reminder with weather context
    public void showPackingReminderWithWeather(String destination, int incompleteItems, String weatherInfo) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String message = "You have " + incompleteItems + " items left to pack for " + destination;
        if (weatherInfo != null && !weatherInfo.isEmpty()) {
            message += ". Current weather: " + weatherInfo;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, PACKING_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Packing Reminder")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        showNotification(builder, PACKING_NOTIFICATION_ID);
    }

    // Start monitoring weather changes for a trip
    public void startWeatherMonitoring(String destination, int duration, String tripType,
                                       String startDate, String endDate, long tripId) {

        // Only start monitoring if we have notification permission
        if (!hasNotificationPermission()) {
            android.util.Log.w("EnhancedNotificationService",
                    "Cannot start weather monitoring without notification permission");
            return;
        }

        // Check weather every 2 hours
        scheduler.scheduleAtFixedRate(() -> {
            weatherService.getDetailedWeather(destination, new WeatherService.WeatherCallback() {
                @Override
                public void onSuccess(WeatherService.WeatherData weatherData) {
                    checkForWeatherAlerts(weatherData, destination, String.valueOf(tripId));

                    // Also check for updated AI recommendations
                    checkForRecommendationUpdates(destination, duration, tripType,
                            startDate, endDate, String.valueOf(tripId));
                }

                @Override
                public void onError(String error) {
                    // Silently handle errors in background monitoring
                }
            });
        }, 0, 2, TimeUnit.HOURS);
    }

    private void checkForWeatherAlerts(WeatherService.WeatherData weatherData, String destination, String tripId) {
        StringBuilder alerts = new StringBuilder();

        // Check for severe weather conditions
        if (weatherData.currentTemp < 0) {
            alerts.append("Freezing temperatures expected! ");
        } else if (weatherData.currentTemp > 35) {
            alerts.append("Extreme heat warning! ");
        }

        if (weatherData.windSpeed > 15) {
            alerts.append("Strong winds forecasted! ");
        }

        // Check hourly forecasts for precipitation
        boolean heavyRainExpected = false;
        for (WeatherService.HourlyForecast forecast : weatherData.hourlyForecasts) {
            if (forecast.precipitationChance > 80) {
                heavyRainExpected = true;
                break;
            }
        }

        if (heavyRainExpected) {
            alerts.append("Heavy rain expected in the next 24 hours! ");
        }

        String condition = weatherData.currentCondition.toLowerCase();
        if (condition.contains("storm") || condition.contains("thunder")) {
            alerts.append("Thunderstorms in the forecast! ");
        }

        if (alerts.length() > 0) {
            String alertMessage = alerts.toString() + "Check updated packing recommendations.";
            showWeatherAlert(destination, alertMessage, tripId);
        }
    }

    private void checkForRecommendationUpdates(String destination, int duration, String tripType,
                                               String startDate, String endDate, String tripId) {

        aiService.generateSmartRecommendations(destination, duration, tripType, startDate, endDate,
                new AIRecommendationService.RecommendationCallback() {
                    @Override
                    public void onSuccess(AIRecommendationService.RecommendationData recommendations) {
                        // Check if there are important notifications
                        if (!recommendations.notifications.isEmpty()) {
                            String updateMessage = String.join(". ", recommendations.notifications);
                            showRecommendationUpdate(destination, updateMessage, tripId);
                        }

                        // Check for weather alerts in recommendations
                        if (recommendations.weatherAlert != null && !recommendations.weatherAlert.isEmpty()) {
                            showWeatherAlert(destination, recommendations.weatherAlert, tripId);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        // Silently handle errors
                    }
                });
    }

    public void stopWeatherMonitoring() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    // Call this from your Application class or main activity
    public static class WeatherMonitoringManager {
        private static EnhancedNotificationService instance;

        public static void startMonitoring(Context context, String destination, int duration,
                                           String tripType, String startDate, String endDate, long tripId) {
            if (instance == null) {
                instance = new EnhancedNotificationService(context.getApplicationContext());
            }
            instance.startWeatherMonitoring(destination, duration, tripType, startDate, endDate, tripId);
        }

        public static void stopMonitoring() {
            if (instance != null) {
                instance.stopWeatherMonitoring();
                instance = null;
            }
        }
    }
}