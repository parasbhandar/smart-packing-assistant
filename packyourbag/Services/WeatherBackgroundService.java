package com.example.packyourbag.Services;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import androidx.core.content.ContextCompat;
import androidx.room.Room;
import com.example.packyourbag.Database.PackingDatabase;
import com.example.packyourbag.DatabaseEntities.Trip;
import com.example.packyourbag.DatabaseEntities.PackingItem;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WeatherBackgroundService extends Service {
    private ScheduledExecutorService scheduler;
    private PackingDatabase database;
    private WeatherService weatherService;
    private AIRecommendationService aiService;
    private EnhancedNotificationService notificationService;

    @Override
    public void onCreate() {
        super.onCreate();

        database = Room.databaseBuilder(getApplicationContext(),
                        PackingDatabase.class, "packing_db")
                .allowMainThreadQueries()
                .build();

        weatherService = new WeatherService(this);
        aiService = new AIRecommendationService(this);
        notificationService = new EnhancedNotificationService(this);

        scheduler = Executors.newScheduledThreadPool(2);

        // Only start monitoring if we have notification permission
        if (hasNotificationPermission()) {
            startWeatherMonitoring();
        } else {
            android.util.Log.w("WeatherBackgroundService",
                    "Cannot start background monitoring without notification permission");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // Restart if killed by system
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }

    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void startWeatherMonitoring() {
        // Check weather updates every 2 hours
        scheduler.scheduleAtFixedRate(() -> {
            checkAllActiveTrips();
        }, 0, 2, TimeUnit.HOURS);

        // Check for packing reminders daily at 9 AM
        scheduler.scheduleAtFixedRate(() -> {
            sendPackingReminders();
        }, getInitialDelay(), 24, TimeUnit.HOURS);
    }

    private long getInitialDelay() {
        // Calculate delay until next 9 AM
        java.util.Calendar now = java.util.Calendar.getInstance();
        java.util.Calendar nextRun = java.util.Calendar.getInstance();

        nextRun.set(java.util.Calendar.HOUR_OF_DAY, 9);
        nextRun.set(java.util.Calendar.MINUTE, 0);
        nextRun.set(java.util.Calendar.SECOND, 0);

        if (nextRun.before(now)) {
            nextRun.add(java.util.Calendar.DAY_OF_MONTH, 1);
        }

        return nextRun.getTimeInMillis() - now.getTimeInMillis();
    }

    private void checkAllActiveTrips() {
        // Double-check permission before processing
        if (!hasNotificationPermission()) {
            android.util.Log.w("WeatherBackgroundService",
                    "Notification permission revoked, stopping monitoring");
            stopSelf();
            return;
        }

        List<Trip> trips = database.tripDao().getAllTrips();

        for (Trip trip : trips) {
            // Only check upcoming trips (within next 30 days)
            if (isUpcomingTrip(trip.startDate)) {
                checkTripWeatherUpdates(trip);
            }
        }
    }

    private boolean isUpcomingTrip(String startDate) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd",
                    java.util.Locale.getDefault());
            java.util.Date tripDate = sdf.parse(startDate);
            java.util.Date today = new java.util.Date();
            java.util.Date thirtyDaysFromNow = new java.util.Date(today.getTime() + 30L * 24 * 60 * 60 * 1000);

            return tripDate != null && tripDate.after(today) && tripDate.before(thirtyDaysFromNow);
        } catch (Exception e) {
            return false;
        }
    }

    private void checkTripWeatherUpdates(Trip trip) {
        String cityName = extractCityName(trip.destination);

        weatherService.getDetailedWeather(cityName, new WeatherService.WeatherCallback() {
            @Override
            public void onSuccess(WeatherService.WeatherData weatherData) {
                // Check for significant weather changes
                if (hasSignificantWeatherChange(trip.weatherInfo, weatherData)) {
                    // Update trip weather info
                    trip.weatherInfo = weatherService.formatWeatherSummary(weatherData);
                    database.tripDao().updateTrip(trip);

                    // Generate updated recommendations
                    checkForUpdatedRecommendations(trip, weatherData);
                }
            }

            @Override
            public void onError(String error) {
                // Log error but don't notify user for background updates
                android.util.Log.e("WeatherBackgroundService",
                        "Weather update failed for " + trip.destination + ": " + error);
            }
        });
    }

    private String extractCityName(String destination) {
        // Extract city name from "City, Country" format
        if (destination.contains(",")) {
            return destination.split(",")[0].trim();
        }
        return destination;
    }

    private boolean hasSignificantWeatherChange(String oldWeatherInfo, WeatherService.WeatherData newWeatherData) {
        if (oldWeatherInfo == null || oldWeatherInfo.equals("Weather data unavailable")) {
            return true;
        }

        // Check for temperature changes > 10 degrees
        // Check for condition changes (sunny to rainy, etc.)
        // This is a simplified check - you can make it more sophisticated

        String newCondition = newWeatherData.currentDescription.toLowerCase();
        String oldInfo = oldWeatherInfo.toLowerCase();

        // Check for severe weather conditions
        if (newCondition.contains("storm") || newCondition.contains("snow") ||
                newCondition.contains("heavy rain")) {
            return true;
        }

        // Check for significant temperature changes
        if (newWeatherData.currentTemp < 0 || newWeatherData.currentTemp > 35) {
            return true;
        }

        return false;
    }

    private void checkForUpdatedRecommendations(Trip trip, WeatherService.WeatherData weatherData) {
        aiService.generateSmartRecommendations(
                extractCityName(trip.destination),
                trip.duration,
                trip.tripType,
                trip.startDate,
                trip.endDate,
                new AIRecommendationService.RecommendationCallback() {
                    @Override
                    public void onSuccess(AIRecommendationService.RecommendationData recommendations) {
                        // Check if new important items need to be added
                        List<String> newItems = getNewRecommendedItems(trip.id, recommendations);

                        if (!newItems.isEmpty()) {
                            // Add new items to database
                            for (String item : newItems) {
                                database.packingItemDao().insertItem(
                                        new PackingItem(trip.id, item, "Weather-Update", false));
                            }

                            // Notify user about new recommendations
                            String message = "Weather conditions have changed. " + newItems.size() +
                                    " new items recommended for your trip to " + trip.destination;
                            notificationService.showRecommendationUpdate(
                                    trip.destination, message, String.valueOf(trip.id));
                        }

                        // Check for weather alerts
                        if (recommendations.weatherAlert != null && !recommendations.weatherAlert.isEmpty()) {
                            notificationService.showWeatherAlert(
                                    trip.destination, recommendations.weatherAlert, String.valueOf(trip.id));
                        }
                    }

                    @Override
                    public void onError(String error) {
                        // Silently handle errors for background updates
                        android.util.Log.e("WeatherBackgroundService",
                                "AI recommendation update failed: " + error);
                    }
                });
    }

    private List<String> getNewRecommendedItems(long tripId, AIRecommendationService.RecommendationData recommendations) {
        List<PackingItem> existingItems = database.packingItemDao().getItemsForTrip(tripId);
        List<String> existingItemNames = new java.util.ArrayList<>();

        for (PackingItem item : existingItems) {
            existingItemNames.add(item.itemName.toLowerCase());
        }

        List<String> newItems = new java.util.ArrayList<>();

        // Check weather-specific items
        for (String item : recommendations.weatherSpecificItems) {
            if (!existingItemNames.contains(item.toLowerCase())) {
                newItems.add(item);
            }
        }

        // Check safety items
        for (String item : recommendations.safetyItems) {
            if (!existingItemNames.contains(item.toLowerCase())) {
                newItems.add(item);
            }
        }

        return newItems;
    }

    private void sendPackingReminders() {
        // Double-check permission before sending notifications
        if (!hasNotificationPermission()) {
            android.util.Log.w("WeatherBackgroundService",
                    "Notification permission revoked, cannot send reminders");
            return;
        }

        List<Trip> upcomingTrips = getUpcomingTripsWithIncompleteItems();

        for (Trip trip : upcomingTrips) {
            List<PackingItem> items = database.packingItemDao().getItemsForTrip(trip.id);
            int incompleteItems = 0;
            for (PackingItem item : items) {
                if (!item.isPacked) incompleteItems++;
            }

            if (incompleteItems > 0) {
                notificationService.showPackingReminderWithWeather(
                        trip.destination, incompleteItems, trip.weatherInfo);
            }
        }
    }

    private List<Trip> getUpcomingTripsWithIncompleteItems() {
        List<Trip> allTrips = database.tripDao().getAllTrips();
        List<Trip> upcomingTrips = new java.util.ArrayList<>();

        for (Trip trip : allTrips) {
            if (isUpcomingTrip(trip.startDate)) {
                // Check if trip has incomplete items
                List<PackingItem> items = database.packingItemDao().getItemsForTrip(trip.id);
                for (PackingItem item : items) {
                    if (!item.isPacked) {
                        upcomingTrips.add(trip);
                        break;
                    }
                }
            }
        }

        return upcomingTrips;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
}