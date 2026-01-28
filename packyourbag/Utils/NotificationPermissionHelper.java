package com.example.packyourbag.Utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class NotificationPermissionHelper {
    public static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;

    /**
     * Check if POST_NOTIFICATIONS permission is granted
     */
    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context,
                    android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // For Android 12 and below, permission is granted by default
    }

    /**
     * Check if we should show rationale for POST_NOTIFICATIONS permission
     */
    public static boolean shouldShowNotificationPermissionRationale(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    android.Manifest.permission.POST_NOTIFICATIONS);
        }
        return false;
    }

    /**
     * Request POST_NOTIFICATIONS permission
     */
    public static void requestNotificationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * Handle the permission result in your Activity's onRequestPermissionsResult method
     */
    public static boolean handlePermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                return true; // Permission granted
            } else {
                return false; // Permission denied
            }
        }
        return false;
    }

    /**
     * Log permission status for debugging
     */
    public static void logPermissionStatus(Context context) {
        boolean hasPermission = hasNotificationPermission(context);
        android.util.Log.d("NotificationPermission",
                "POST_NOTIFICATIONS permission granted: " + hasPermission);
    }
}