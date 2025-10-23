package com.mapointeuse.service

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.mapointeuse.data.WorkPlace

/**
 * Gestionnaire du Geofencing natif Android.
 * Utilise l'API Google Play Services Location pour une détection économe en batterie.
 * Fonctionne en permanence en arrière-plan, même si l'app est fermée.
 */
class NativeGeofencingManager(private val context: Context) {

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    companion object {
        private const val TAG = "NativeGeofencing"
        private const val GEOFENCE_REQUEST_ID_PREFIX = "workplace_"
    }

    /**
     * Enregistre un geofence pour un lieu de travail.
     * Le système Android surveillera automatiquement cette zone.
     */
    fun registerGeofence(workPlace: WorkPlace) {
        if (!checkPermissions()) {
            Log.e(TAG, "Missing location permissions for geofencing")
            return
        }

        // Créer le geofence
        val geofence = Geofence.Builder()
            .setRequestId(GEOFENCE_REQUEST_ID_PREFIX + workPlace.id)
            .setCircularRegion(
                workPlace.latitude,
                workPlace.longitude,
                workPlace.radiusMeters.toFloat()
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
            )
            // Définir un délai pour éviter les faux positifs
            .setLoiteringDelay(60000) // 1 minute
            .build()

        // Créer la requête de geofencing
        val geofencingRequest = GeofencingRequest.Builder().apply {
            // Déclencher ENTER si l'utilisateur est déjà dans la zone au moment de l'enregistrement
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofence(geofence)
        }.build()

        // Enregistrer le geofence
        try {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "Geofence registered successfully for ${workPlace.name}")
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to register geofence: ${exception.message}", exception)
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException when registering geofence", e)
        }
    }

    /**
     * Supprime un geofence pour un lieu de travail.
     */
    fun unregisterGeofence(workPlaceId: Long) {
        val requestId = GEOFENCE_REQUEST_ID_PREFIX + workPlaceId

        geofencingClient.removeGeofences(listOf(requestId))
            .addOnSuccessListener {
                Log.d(TAG, "Geofence unregistered successfully: $requestId")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to unregister geofence: ${exception.message}", exception)
            }
    }

    /**
     * Supprime tous les geofences enregistrés.
     */
    fun unregisterAllGeofences() {
        geofencingClient.removeGeofences(geofencePendingIntent)
            .addOnSuccessListener {
                Log.d(TAG, "All geofences unregistered successfully")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to unregister all geofences: ${exception.message}", exception)
            }
    }

    /**
     * PendingIntent qui sera déclenché par le système Android quand l'utilisateur
     * entre ou sort de la zone de geofencing.
     */
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    /**
     * Vérifie si les permissions nécessaires sont accordées.
     */
    private fun checkPermissions(): Boolean {
        val fineLocation = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val backgroundLocation = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Pas nécessaire avant Android 10
        }

        return fineLocation && backgroundLocation
    }
}
