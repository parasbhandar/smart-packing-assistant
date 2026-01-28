package com.example.packyourbag.Utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PackingUtils {

    public static boolean isUpcomingTrip(String startDateString, int daysAhead) {
        // Add null and empty string checks
        if (startDateString == null || startDateString.trim().isEmpty()) {
            return false;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date tripDate = sdf.parse(startDateString);
            if (tripDate == null) {
                return false;
            }

            Date currentDate = new Date();
            long diffInMillies = tripDate.getTime() - currentDate.getTime();
            long diffInDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);

            return diffInDays <= daysAhead && diffInDays >= 0;
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static int calculateDuration(String startDate, String endDate) {
        // Add null checks
        if (startDate == null || endDate == null ||
                startDate.trim().isEmpty() || endDate.trim().isEmpty()) {
            return 1; // Default to 1 day
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date start = sdf.parse(startDate);
            Date end = sdf.parse(endDate);

            if (start != null && end != null) {
                long diffInMillies = end.getTime() - start.getTime();
                return (int) TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS) + 1;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 1; // Default to 1 day
    }

    public static String getPackingProgress(int packedItems, int totalItems) {
        if (totalItems == 0) return "No items";
        int percentage = (packedItems * 100) / totalItems;
        return packedItems + "/" + totalItems + " items (" + percentage + "%)";
    }

    public static String getWeatherEmoji(String condition) {
        if (condition == null || condition.trim().isEmpty()) {
            return "🌤️"; // Default weather emoji
        }

        condition = condition.toLowerCase();
        if (condition.contains("rain")) return "🌧️";
        if (condition.contains("snow")) return "❄️";
        if (condition.contains("cloud")) return "☁️";
        if (condition.contains("sun") || condition.contains("clear")) return "☀️";
        if (condition.contains("storm")) return "⛈️";
        return "🌤️";
    }

    public static String formatDateRange(String startDate, String endDate) {
        // Add null checks
        if (startDate == null) startDate = "Unknown";
        if (endDate == null) endDate = "Unknown";
        return startDate + " to " + endDate;
    }

    public static boolean isTripActive(String startDate, String endDate) {
        // Add null checks
        if (startDate == null || endDate == null ||
                startDate.trim().isEmpty() || endDate.trim().isEmpty()) {
            return false;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date start = sdf.parse(startDate);
            Date end = sdf.parse(endDate);
            Date today = new Date();

            return start != null && end != null &&
                    today.compareTo(start) >= 0 && today.compareTo(end) <= 0;
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }
}