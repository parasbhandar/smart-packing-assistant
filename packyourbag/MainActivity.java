package com.example.packyourbag;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.packyourbag.Adapter.TripAdapter;
import com.example.packyourbag.Database.PackingDatabase;
import com.example.packyourbag.DatabaseEntities.PackingItem;
import com.example.packyourbag.DatabaseEntities.Trip;
import com.example.packyourbag.Utils.PackingUtils;
import com.example.packyourbag.Services.WeatherService;
import com.example.packyourbag.Services.AIRecommendationService;
import com.example.packyourbag.Services.EnhancedNotificationService;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private PackingDatabase database;
    private TextView textDuration, textStartDate, textEndDate;
    private Spinner spinnerTripType;
    private Button btnCreateTrip, btnSelectStartDate, btnSelectEndDate, btnViewOverview;
    private RecyclerView recyclerTrips;
    private TripAdapter tripAdapter;
    private ProgressBar progressBar;
    private String startDate, endDate;
    private AutoCompleteTextView editDestination;
    private ProgressBar cityLoader;
    private int calculatedDuration = 0;
    private static final String API_KEY = "7e73becad5526e8ca1fb06b3a9d2bd91";

    // Permission launcher for POST_NOTIFICATIONS
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    private TextWatcher textWatcher;
    private boolean suppressWatcher = false;            // prevents loops
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable fetchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize permission launcher
        initPermissionLauncher();

        // Initialize database with migration
        database = Room.databaseBuilder(getApplicationContext(),
                        PackingDatabase.class, "packing_db")
                .addMigrations(PackingDatabase.MIGRATION_1_2)
                .allowMainThreadQueries()
                .build();

        initViews();
        setupRecyclerView();
        loadTrips();

        // Create notification channels for enhanced features
        EnhancedNotificationService.createNotificationChannels(this);

        // Check and request notification permission
        checkNotificationPermission();

        checkUpcomingTrips();
    }

    private void initPermissionLauncher() {
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Toast.makeText(this, "Notification permission granted! You'll receive trip reminders.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Notification permission denied. You won't receive trip reminders.", Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        android.Manifest.permission.POST_NOTIFICATIONS)) {
                    // Show explanation dialog
                    showNotificationPermissionRationale();
                } else {
                    // Request permission directly
                    requestNotificationPermission();
                }
            }
        }
    }

    private void showNotificationPermissionRationale() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Enable Notifications")
                .setMessage("This app sends helpful notifications about:\n\n" +
                        "• Weather alerts for your trips\n" +
                        "• Packing reminders\n" +
                        "• AI-powered recommendations\n\n" +
                        "Would you like to enable notifications?")
                .setPositiveButton("Enable", (dialog, which) -> requestNotificationPermission())
                .setNegativeButton("Not Now", null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void initViews() {
        editDestination = findViewById(R.id.editDestination);
        cityLoader = findViewById(R.id.cityLoader);
        setDestinationAddTextChangeListener();
        // Handle selection
        editDestination.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCity = (String) parent.getItemAtPosition(position);
            suppressWatcher = true;
            editDestination.setText(selectedCity);
            editDestination.setSelection(selectedCity.length());
            editDestination.dismissDropDown();
            editDestination.clearFocus();

            // Re-enable after the completion finishes
            editDestination.post(() -> suppressWatcher = false);
        });

        textDuration = findViewById(R.id.textDuration);
        textStartDate = findViewById(R.id.textStartDate);
        textEndDate = findViewById(R.id.textEndDate);
        spinnerTripType = findViewById(R.id.spinnerTripType);
        btnCreateTrip = findViewById(R.id.btnCreateTrip);
        btnSelectStartDate = findViewById(R.id.btnSelectStartDate);
        btnSelectEndDate = findViewById(R.id.btnSelectEndDate);
        btnViewOverview = findViewById(R.id.btnViewOverview);
        recyclerTrips = findViewById(R.id.recyclerTrips);
        progressBar = findViewById(R.id.progressBar);

        // Setup trip type spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.trip_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTripType.setAdapter(adapter);

        btnSelectStartDate.setOnClickListener(v -> showStartDatePicker());
        btnSelectEndDate.setOnClickListener(v -> showEndDatePicker());
        btnCreateTrip.setOnClickListener(v -> createTrip());
        btnViewOverview.setOnClickListener(v -> openOverview());

        // Initially disable end date and create trip buttons
        btnSelectEndDate.setEnabled(false);
        btnCreateTrip.setEnabled(false);
    }

    private void setDestinationAddTextChangeListener() {
        if (textWatcher != null) return; // don't add multiple times

        fetchRunnable = () -> {
            String q = editDestination.getText().toString().trim();
            if (q.length() >= 1) {
                fetchCitySuggestions(q);  // make sure this DOES NOT call setText()
            }
        };

        textWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 1) Ignore programmatic changes
                if (suppressWatcher) return;

                // 2) Ignore changes caused by selecting a suggestion
                if (editDestination.isPerformingCompletion()) return;

                // 3) Debounce network calls
                handler.removeCallbacks(fetchRunnable);
                if (s.length() >= 1) {
                    handler.postDelayed(fetchRunnable, 300);
                } else {
                    handler.removeCallbacks(fetchRunnable);
                }
            }

            @Override public void afterTextChanged(Editable s) {}
        };

        editDestination.addTextChangedListener(textWatcher);
    }

    private void openOverview() {
        Intent intent = new Intent(this, OverviewActivity.class);
        startActivity(intent);
    }

    private void fetchCitySuggestions(String query) {
        cityLoader.setVisibility(View.VISIBLE);

        String url = "https://api.openweathermap.org/geo/1.0/direct?q=" + query +
                "&limit=5&appid=" + API_KEY;

        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    cityLoader.setVisibility(View.GONE);
                    try {
                        JSONArray arr = new JSONArray(response);
                        List<String> suggestions = new ArrayList<>();
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            String city = obj.getString("name");
                            String country = obj.getString("country");
                            suggestions.add(city + ", " + country);
                        }
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                MainActivity.this,
                                android.R.layout.simple_dropdown_item_1line,
                                suggestions
                        );
                        editDestination.setAdapter(adapter);;
                        if (!suggestions.isEmpty() && editDestination.hasFocus()) {
                            editDestination.showDropDown();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    cityLoader.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Error fetching cities", Toast.LENGTH_SHORT).show();
                });

        queue.add(request);
    }

    private void showStartDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    startDate = year + "-" + String.format("%02d", month + 1) + "-" + String.format("%02d", dayOfMonth);
                    textStartDate.setText("Start: " + startDate);
                    btnSelectEndDate.setEnabled(true);
                    calculateDuration();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void showEndDatePicker() {
        if (startDate == null) {
            Toast.makeText(this, "Please select start date first", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    endDate = year + "-" + String.format("%02d", month + 1) + "-" + String.format("%02d", dayOfMonth);
                    textEndDate.setText("End: " + endDate);
                    calculateDuration();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        // Set minimum date to start date
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date start = sdf.parse(startDate);
            if (start != null) {
                datePickerDialog.getDatePicker().setMinDate(start.getTime());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        datePickerDialog.show();
    }

    private void calculateDuration() {
        if (startDate != null && endDate != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date start = sdf.parse(startDate);
                Date end = sdf.parse(endDate);

                if (start != null && end != null) {
                    long diffInMillies = end.getTime() - start.getTime();
                    calculatedDuration = (int) TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS) + 1; // +1 to include both days

                    textDuration.setText("Duration: " + calculatedDuration + " days");
                    textDuration.setVisibility(View.VISIBLE);
                    btnCreateTrip.setEnabled(true);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error calculating duration", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void createTrip() {
        String destination = editDestination.getText().toString().trim();
        String tripType = spinnerTripType.getSelectedItem().toString();

        if (destination.isEmpty() || startDate == null || endDate == null) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        createTripWithEnhancedFeatures(destination, tripType);
    }

    private void createTripWithEnhancedFeatures(String destination, String tripType) {
        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        btnCreateTrip.setEnabled(false);

        // Use the new weather service for detailed weather data
        WeatherService weatherService = new WeatherService(this);
        weatherService.getDetailedWeather(destination, new WeatherService.WeatherCallback() {
            @Override
            public void onSuccess(WeatherService.WeatherData weatherData) {
                // Create trip with enhanced weather data
                String fullDestination = weatherData.cityName + ", " + weatherData.country;
                String weatherInfo = weatherService.formatWeatherSummary(weatherData);

                Trip trip = new Trip(fullDestination, calculatedDuration, tripType,
                        startDate, endDate, weatherInfo, System.currentTimeMillis());
                long tripId = database.tripDao().insertTrip(trip);

                // Generate AI-powered recommendations
                generateAIRecommendations(tripId, destination, tripType, weatherData);

                // Start weather monitoring for this trip (only if permission is granted)
                if (hasNotificationPermission()) {
                    EnhancedNotificationService.WeatherMonitoringManager.startMonitoring(
                            MainActivity.this, destination, calculatedDuration, tripType,
                            startDate, endDate, tripId);
                }

                progressBar.setVisibility(View.GONE);
                btnCreateTrip.setEnabled(true);
            }

            @Override
            public void onError(String error) {
                // Fallback to original method
                handleWeatherError(destination, tripType);
            }
        });
    }

    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void generateAIRecommendations(long tripId, String destination, String tripType,
                                           WeatherService.WeatherData weatherData) {

        AIRecommendationService aiService = new AIRecommendationService(this);
        aiService.generateSmartRecommendations(destination, calculatedDuration, tripType,
                startDate, endDate,
                new AIRecommendationService.RecommendationCallback() {
                    @Override
                    public void onSuccess(AIRecommendationService.RecommendationData recommendations) {
                        // Add AI-generated items to the database
                        addAIRecommendationsToDatabase(tripId, recommendations);

                        // Show notification if there are important alerts - ADD NULL CHECK
                        if (hasNotificationPermission() && recommendations.weatherAlert != null && !recommendations.weatherAlert.isEmpty()) {
                            EnhancedNotificationService notificationService =
                                    new EnhancedNotificationService(MainActivity.this);
                            notificationService.showWeatherAlert(destination,
                                    recommendations.weatherAlert,
                                    String.valueOf(tripId));
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent(MainActivity.this, PackingListActivity.class);
                                intent.putExtra("tripId", tripId);
                                startActivity(intent);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        // Fallback to basic recommendations
                        generateSmartSuggestions(tripId, tripType, calculatedDuration, 20.0, "Clear", 50);
                        Toast.makeText(MainActivity.this,
                                "Using basic recommendations: " + error, Toast.LENGTH_SHORT).show();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent(MainActivity.this, PackingListActivity.class);
                                intent.putExtra("tripId", tripId);
                                startActivity(intent);
                            }
                        });
                    }
                });
    }

    private void addAIRecommendationsToDatabase(long tripId, AIRecommendationService.RecommendationData recommendations) {
        // Add essential items
        for (String item : recommendations.essentialItems) {
            database.packingItemDao().insertItem(
                    new PackingItem(tripId, item, "Essential", false));
        }

        // Add weather-specific items
        for (String item : recommendations.weatherSpecificItems) {
            database.packingItemDao().insertItem(
                    new PackingItem(tripId, item, "Weather-Specific", false));
        }

        // Add activity-based items
        for (String item : recommendations.activityBasedItems) {
            database.packingItemDao().insertItem(
                    new PackingItem(tripId, item, "Activity", false));
        }

        // Add safety items
        for (String item : recommendations.safetyItems) {
            database.packingItemDao().insertItem(
                    new PackingItem(tripId, item, "Safety", false));
        }
    }

    private void fetchWeatherAndCreateTrip(String destination, String tripType) {
        String url = "https://api.openweathermap.org/data/2.5/weather?q=" + destination + "&appid=" + API_KEY + "&units=metric";

        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        JSONObject main = jsonObject.getJSONObject("main");
                        JSONObject weather = jsonObject.getJSONArray("weather").getJSONObject(0);

                        // Get actual city name from API response
                        String actualDestination = jsonObject.getString("name");
                        String country = jsonObject.getJSONObject("sys").getString("country");
                        String fullDestination = actualDestination + ", " + country;

                        double temp = main.getDouble("temp");
                        int humidity = main.getInt("humidity");
                        String weatherCondition = weather.getString("main");
                        String description = weather.getString("description");

                        // Create weather info string
                        String weatherInfo = "Temperature: " + Math.round(temp) + "°C, " +
                                "Condition: " + description + ", " +
                                "Humidity: " + humidity + "%";

                        // Create trip with weather data
                        Trip trip = new Trip(fullDestination, calculatedDuration, tripType,
                                startDate, endDate, weatherInfo, System.currentTimeMillis());
                        long tripId = database.tripDao().insertTrip(trip);

                        // Generate smart suggestions based on weather
                        generateSmartSuggestions(tripId, tripType, calculatedDuration, temp, weatherCondition, humidity);

                        progressBar.setVisibility(View.GONE);
                        btnCreateTrip.setEnabled(true);

                        Intent intent = new Intent(MainActivity.this, PackingListActivity.class);
                        intent.putExtra("tripId", tripId);
                        startActivity(intent);

                    } catch (Exception e) {
                        handleWeatherError(destination, tripType);
                    }
                },
                error -> handleWeatherError(destination, tripType));

        queue.add(stringRequest);
    }

    private void handleWeatherError(String destination, String tripType) {
        // Create trip without weather data
        Trip trip = new Trip(destination, calculatedDuration, tripType,
                startDate, endDate, "Weather data unavailable", System.currentTimeMillis());
        long tripId = database.tripDao().insertTrip(trip);

        // Generate basic suggestions
        generateSmartSuggestions(tripId, tripType, calculatedDuration, 20.0, "Clear", 50);

        progressBar.setVisibility(View.GONE);
        btnCreateTrip.setEnabled(true);

        Toast.makeText(this, "Trip created without weather data", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, PackingListActivity.class);
        intent.putExtra("tripId", tripId);
        startActivity(intent);
    }

    private void generateSmartSuggestions(long tripId, String tripType, int duration, double temp, String weatherCondition, int humidity) {
        // Basic items for all trips
        database.packingItemDao().insertItem(new PackingItem(tripId, "Underwear (" + duration + " pairs)", "Clothing", false));
        database.packingItemDao().insertItem(new PackingItem(tripId, "Socks (" + duration + " pairs)", "Clothing", false));
        database.packingItemDao().insertItem(new PackingItem(tripId, "Toothbrush", "Personal Care", false));
        database.packingItemDao().insertItem(new PackingItem(tripId, "Toothpaste", "Personal Care", false));
        database.packingItemDao().insertItem(new PackingItem(tripId, "Phone Charger", "Electronics", false));
        database.packingItemDao().insertItem(new PackingItem(tripId, "Passport/ID", "Documents", false));

        // Trip-specific suggestions
        switch (tripType.toLowerCase()) {
            case "business":
                database.packingItemDao().insertItem(new PackingItem(tripId, "Formal Suits (" + Math.min(duration, 3) + ")", "Clothing", false));
                database.packingItemDao().insertItem(new PackingItem(tripId, "Dress Shoes", "Clothing", false));
                database.packingItemDao().insertItem(new PackingItem(tripId, "Business Cards", "Documents", false));
                database.packingItemDao().insertItem(new PackingItem(tripId, "Laptop", "Electronics", false));
                database.packingItemDao().insertItem(new PackingItem(tripId, "Laptop Charger", "Electronics", false));
                break;
            case "beach":
                database.packingItemDao().insertItem(new PackingItem(tripId, "Swimsuit (2)", "Clothing", false));
                database.packingItemDao().insertItem(new PackingItem(tripId, "Sunscreen SPF 30+", "Personal Care", false));
                database.packingItemDao().insertItem(new PackingItem(tripId, "Beach Towel", "Accessories", false));
                database.packingItemDao().insertItem(new PackingItem(tripId, "Flip Flops", "Clothing", false));
                database.packingItemDao().insertItem(new PackingItem(tripId, "Sunglasses", "Accessories", false));
                break;
            case "adventure":
                database.packingItemDao().insertItem(new PackingItem(tripId, "Hiking Boots", "Clothing", false));
                database.packingItemDao().insertItem(new PackingItem(tripId, "First Aid Kit", "Safety", false));
                database.packingItemDao().insertItem(new PackingItem(tripId, "Water Bottle", "Accessories", false));
                database.packingItemDao().insertItem(new PackingItem(tripId, "Backpack", "Accessories", false));
                database.packingItemDao().insertItem(new PackingItem(tripId, "Flashlight", "Safety", false));
                break;
        }

        // Weather-based suggestions
        if (temp < 10) {
            database.packingItemDao().insertItem(new PackingItem(tripId, "Heavy Winter Jacket", "Clothing", false));
            database.packingItemDao().insertItem(new PackingItem(tripId, "Gloves", "Clothing", false));
            database.packingItemDao().insertItem(new PackingItem(tripId, "Warm Hat", "Clothing", false));
            database.packingItemDao().insertItem(new PackingItem(tripId, "Scarf", "Clothing", false));
        } else if (temp < 20) {
            database.packingItemDao().insertItem(new PackingItem(tripId, "Light Jacket", "Clothing", false));
            database.packingItemDao().insertItem(new PackingItem(tripId, "Long Pants", "Clothing", false));
            database.packingItemDao().insertItem(new PackingItem(tripId, "Closed Shoes", "Clothing", false));
        } else if (temp >= 30) {
            database.packingItemDao().insertItem(new PackingItem(tripId, "Light T-shirts", "Clothing", false));
            database.packingItemDao().insertItem(new PackingItem(tripId, "Shorts", "Clothing", false));
            database.packingItemDao().insertItem(new PackingItem(tripId, "Sandals", "Clothing", false));
            database.packingItemDao().insertItem(new PackingItem(tripId, "Extra Sunscreen", "Personal Care", false));
        }

        // Condition-based suggestions
        if (weatherCondition.toLowerCase().contains("rain")) {
            database.packingItemDao().insertItem(new PackingItem(tripId, "Rain Jacket", "Clothing", false));
            database.packingItemDao().insertItem(new PackingItem(tripId, "Umbrella", "Accessories", false));
            database.packingItemDao().insertItem(new PackingItem(tripId, "Waterproof Shoes", "Clothing", false));
        }

        if (weatherCondition.toLowerCase().contains("snow")) {
            database.packingItemDao().insertItem(new PackingItem(tripId, "Winter Boots", "Clothing", false));
            database.packingItemDao().insertItem(new PackingItem(tripId, "Thermal Underwear", "Clothing", false));
        }

        // Humidity-based suggestions
        if (humidity > 70) {
            database.packingItemDao().insertItem(new PackingItem(tripId, "Moisture-wicking Clothes", "Clothing", false));
            database.packingItemDao().insertItem(new PackingItem(tripId, "Extra Deodorant", "Personal Care", false));
        }

        // Duration-based suggestions
        if (duration > 7) {
            database.packingItemDao().insertItem(new PackingItem(tripId, "Laundry Bag", "Accessories", false));
            database.packingItemDao().insertItem(new PackingItem(tripId, "Laundry Detergent Pods", "Accessories", false));
        }

        if (duration > 14) {
            database.packingItemDao().insertItem(new PackingItem(tripId, "Extra Toiletries", "Personal Care", false));
        }
    }

    private void setupRecyclerView() {
        tripAdapter = new TripAdapter(this::openTrip, this::deleteTrip);
        recyclerTrips.setLayoutManager(new LinearLayoutManager(this));
        recyclerTrips.setAdapter(tripAdapter);
    }

    private void openTrip(Trip trip) {
        Intent intent = new Intent(this, PackingListActivity.class);
        intent.putExtra("tripId", trip.id);
        startActivity(intent);
    }

    private void deleteTrip(Trip trip) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Delete Trip")
                .setMessage("Are you sure you want to delete this trip?\n\n" + trip.destination +
                        "\n(" + trip.startDate + " to " + trip.endDate + ")")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Delete all packing items for this trip
                    database.packingItemDao().deleteItemsForTrip(trip.id);
                    // Delete the trip
                    database.tripDao().deleteTrip(trip);
                    // Refresh the list
                    loadTrips();
                    Toast.makeText(MainActivity.this, "Trip deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void loadTrips() {
        List<Trip> trips = database.tripDao().getAllTrips();
        tripAdapter.updateTrips(trips);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTrips();

        // Reset form
        editDestination.setText("");
        textDuration.setVisibility(View.GONE);
        textStartDate.setText("Select Start Date");
        textEndDate.setText("Select End Date");
        startDate = null;
        endDate = null;
        calculatedDuration = 0;
        btnSelectEndDate.setEnabled(false);
        btnCreateTrip.setEnabled(false);
    }

    private void checkUpcomingTrips() {
        // Only send notifications if permission is granted
        if (!hasNotificationPermission()) {
            return;
        }

        List<Trip> trips = database.tripDao().getAllTrips();
        for (Trip trip : trips) {
            // Use trip.startDate instead of trip.date
            if (trip.startDate != null && PackingUtils.isUpcomingTrip(trip.startDate, 3)) { // Check trips in next 3 days
                List<PackingItem> items = database.packingItemDao().getItemsForTrip(trip.id);
                int incompleteItems = 0;
                for (PackingItem item : items) {
                    if (!item.isPacked) incompleteItems++;
                }

                if (incompleteItems > 0) {
                    EnhancedNotificationService notificationService =
                            new EnhancedNotificationService(this);
                    notificationService.showPackingReminderWithWeather(
                            trip.destination, incompleteItems, trip.weatherInfo);
                }
            }
        }
    }
}