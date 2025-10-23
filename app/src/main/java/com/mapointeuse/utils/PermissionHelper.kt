package com.mapointeuse.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionHelper {

    const val LOCATION_PERMISSION_REQUEST_CODE = 1001

    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val BACKGROUND_PERMISSION = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    } else {
        emptyArray()
    }

    private val NOTIFICATION_PERMISSION = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        emptyArray()
    }

    /**
     * Vérifie si toutes les permissions de base sont accordées
     */
    fun hasLocationPermissions(context: Context): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Vérifie si la permission de localisation en arrière-plan est accordée
     */
    fun hasBackgroundLocationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return true // Pas nécessaire avant Android 10
        }
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Vérifie si la permission de notification est accordée
     */
    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true // Pas nécessaire avant Android 13
        }
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Vérifie si toutes les permissions nécessaires sont accordées
     */
    fun hasAllPermissions(context: Context): Boolean {
        return hasLocationPermissions(context) &&
                hasBackgroundLocationPermission(context) &&
                hasNotificationPermission(context)
    }

    /**
     * Demande les permissions de localisation de base
     */
    fun requestLocationPermissions(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            REQUIRED_PERMISSIONS,
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    /**
     * Demande la permission de localisation en arrière-plan
     * IMPORTANT: Doit être demandée séparément après les permissions de base
     */
    fun requestBackgroundLocationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                activity,
                BACKGROUND_PERMISSION,
                LOCATION_PERMISSION_REQUEST_CODE + 1
            )
        }
    }

    /**
     * Demande la permission de notification
     */
    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                NOTIFICATION_PERMISSION,
                LOCATION_PERMISSION_REQUEST_CODE + 2
            )
        }
    }

    /**
     * Demande toutes les permissions nécessaires dans l'ordre approprié
     */
    fun requestAllPermissions(activity: Activity) {
        val permissionsToRequest = mutableListOf<String>()

        // Permissions de localisation de base
        if (!hasLocationPermissions(activity)) {
            permissionsToRequest.addAll(REQUIRED_PERMISSIONS)
        }

        // Permission de notification
        if (!hasNotificationPermission(activity)) {
            permissionsToRequest.addAll(NOTIFICATION_PERMISSION)
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                permissionsToRequest.toTypedArray(),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    /**
     * Vérifie si l'utilisateur doit être redirigé vers les paramètres
     */
    fun shouldShowRationale(activity: Activity): Boolean {
        return REQUIRED_PERMISSIONS.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }
}
