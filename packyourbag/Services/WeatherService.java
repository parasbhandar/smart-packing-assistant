package com.example.packyourbag.Services;

import android.content.Context;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WeatherService {
    private static final String API_KEY = "";
    private static final String CURRENT_WEATHER_URL = "";
    private static final String FORECAST_URL = "";
    private static final String ONECALL_URL = "";

    private Context context;
    private RequestQueue requestQueue;

    public WeatherService(Context context) {
        this.context = context;
        this.requestQueue = Volley.newRequestQueue(context);
    }

    public interface WeatherCallback {
        void onSuccess(WeatherData weatherData);
        void onError(String error);
    }

    public static class WeatherData {
        public String cityName;
        public String country;
        public double currentTemp;
        public String currentCondition;
        public String currentDescription;
        public int humidity;
        public double windSpeed;
        public int visibility;
        public double feelsLike;
        public List<HourlyForecast> hourlyForecasts;
        public List<DailyForecast> dailyForecasts;
        public long timestamp;

        public WeatherData() {
            this.hourlyForecasts = new ArrayList<>();
            this.dailyForecasts = new ArrayList<>();
            this.timestamp = System.currentTimeMillis();
        }
    }

    public static class HourlyForecast {
        public long timestamp;
        public double temperature;
        public String condition;
        public String description;
        public int humidity;
        public double windSpeed;
        public double precipitationChance;

        public String getFormattedTime() {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return sdf.format(new Date(timestamp * 1000));
        }
    }

    public static class DailyForecast {
        public long timestamp;
        public double minTemp;
        public double maxTemp;
        public String condition;
        public String description;
        public int humidity;
        public double precipitationChance;

        public String getFormattedDate() {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
            return sdf.format(new Date(timestamp * 1000));
        }
    }

    public void getDetailedWeather(String cityName, WeatherCallback callback) {
        // First get coordinates from city name
        getCoordinates(cityName, new CoordinatesCallback() {
            @Override
            public void onSuccess(double lat, double lon, String actualCityName, String country) {
                // Then get detailed weather data using coordinates
                getOneCallWeather(lat, lon, actualCityName, country, callback);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    private interface CoordinatesCallback {
        void onSuccess(double lat, double lon, String cityName, String country);
        void onError(String error);
    }

    private void getCoordinates(String cityName, CoordinatesCallback callback) {
        String url = CURRENT_WEATHER_URL + "?q=" + cityName + "&appid=" + API_KEY + "&units=metric";

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        JSONObject coord = json.getJSONObject("coord");
                        double lat = coord.getDouble("lat");
                        double lon = coord.getDouble("lon");
                        String actualCityName = json.getString("name");
                        String country = json.getJSONObject("sys").getString("country");
                        callback.onSuccess(lat, lon, actualCityName, country);
                    } catch (Exception e) {
                        callback.onError("Error parsing coordinates: " + e.getMessage());
                    }
                },
                error -> callback.onError("Failed to get coordinates: " + error.getMessage())
        );

        requestQueue.add(request);
    }

    private void getOneCallWeather(double lat, double lon, String cityName, String country, WeatherCallback callback) {
        // Note: One Call API 3.0 requires subscription, using 5-day forecast as alternative
        String url = FORECAST_URL + "?lat=" + lat + "&lon=" + lon + "&appid=" + API_KEY + "&units=metric";

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        WeatherData weatherData = parseDetailedWeatherResponse(response, cityName, country);
                        callback.onSuccess(weatherData);
                    } catch (Exception e) {
                        callback.onError("Error parsing weather data: " + e.getMessage());
                    }
                },
                error -> callback.onError("Failed to fetch detailed weather: " + error.getMessage())
        );

        requestQueue.add(request);
    }

    private WeatherData parseDetailedWeatherResponse(String response, String cityName, String country) throws Exception {
        JSONObject json = new JSONObject(response);
        WeatherData weatherData = new WeatherData();

        weatherData.cityName = cityName;
        weatherData.country = country;

        JSONArray list = json.getJSONArray("list");

        // Get current weather from first forecast item
        if (list.length() > 0) {
            JSONObject current = list.getJSONObject(0);
            JSONObject main = current.getJSONObject("main");
            JSONObject weather = current.getJSONArray("weather").getJSONObject(0);
            JSONObject wind = current.optJSONObject("wind");

            weatherData.currentTemp = main.getDouble("temp");
            weatherData.currentCondition = weather.getString("main");
            weatherData.currentDescription = weather.getString("description");
            weatherData.humidity = main.getInt("humidity");
            weatherData.feelsLike = main.getDouble("feels_like");
            if (wind != null) {
                weatherData.windSpeed = wind.optDouble("speed", 0);
            }
        }

        // Parse hourly forecasts (next 24 hours)
        for (int i = 0; i < Math.min(8, list.length()); i++) { // 8 * 3 hours = 24 hours
            JSONObject item = list.getJSONObject(i);
            HourlyForecast forecast = new HourlyForecast();

            forecast.timestamp = item.getLong("dt");
            JSONObject main = item.getJSONObject("main");
            JSONObject weather = item.getJSONArray("weather").getJSONObject(0);

            forecast.temperature = main.getDouble("temp");
            forecast.condition = weather.getString("main");
            forecast.description = weather.getString("description");
            forecast.humidity = main.getInt("humidity");

            if (item.has("wind")) {
                forecast.windSpeed = item.getJSONObject("wind").optDouble("speed", 0);
            }
            if (item.has("pop")) {
                forecast.precipitationChance = item.getDouble("pop") * 100;
            }

            weatherData.hourlyForecasts.add(forecast);
        }

        // Parse daily forecasts
        parseDailyForecasts(list, weatherData);

        return weatherData;
    }

    private void parseDailyForecasts(JSONArray list, WeatherData weatherData) throws Exception {
        // Group forecasts by day
        String lastDate = "";
        DailyForecast currentDay = null;

        for (int i = 0; i < list.length(); i++) {
            JSONObject item = list.getJSONObject(i);
            long timestamp = item.getLong("dt");

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String currentDate = dateFormat.format(new Date(timestamp * 1000));

            if (!currentDate.equals(lastDate)) {
                // Start new day
                if (currentDay != null) {
                    weatherData.dailyForecasts.add(currentDay);
                }

                currentDay = new DailyForecast();
                currentDay.timestamp = timestamp;

                JSONObject main = item.getJSONObject("main");
                JSONObject weather = item.getJSONArray("weather").getJSONObject(0);

                currentDay.minTemp = main.getDouble("temp_min");
                currentDay.maxTemp = main.getDouble("temp_max");
                currentDay.condition = weather.getString("main");
                currentDay.description = weather.getString("description");
                currentDay.humidity = main.getInt("humidity");

                if (item.has("pop")) {
                    currentDay.precipitationChance = item.getDouble("pop") * 100;
                }

                lastDate = currentDate;
            } else if (currentDay != null) {
                // Update min/max temperatures for the same day
                JSONObject main = item.getJSONObject("main");
                currentDay.minTemp = Math.min(currentDay.minTemp, main.getDouble("temp_min"));
                currentDay.maxTemp = Math.max(currentDay.maxTemp, main.getDouble("temp_max"));
            }
        }

        // Add the last day
        if (currentDay != null) {
            weatherData.dailyForecasts.add(currentDay);
        }
    }

    public String formatWeatherSummary(WeatherData weatherData) {
        StringBuilder summary = new StringBuilder();
        summary.append("Current: ").append(Math.round(weatherData.currentTemp)).append("°C, ");
        summary.append(weatherData.currentDescription);
        summary.append(" (Feels like ").append(Math.round(weatherData.feelsLike)).append("°C)\n");
        summary.append("Humidity: ").append(weatherData.humidity).append("%\n");
        if (weatherData.windSpeed > 0) {
            summary.append("Wind: ").append(weatherData.windSpeed).append(" m/s\n");
        }
        summary.append("Updated: ").append(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));

        return summary.toString();
    }
}