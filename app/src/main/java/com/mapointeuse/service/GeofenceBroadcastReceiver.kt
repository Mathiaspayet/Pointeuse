package com.mapointeuse.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.mapointeuse.MainActivity
import com.mapointeuse.R

/**
 * BroadcastReceiver qui reçoit les événements de geofencing natif Android.
 * Déclenché automatiquement par le système quand l'utilisateur entre/sort de la zone.
 * Fonctionne même si l'app est fermée - très économe en batterie.
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceBroadcastRcv"
        private const val NOTIFICATION_CHANNEL_ID = "geofencing_native_channel"
        private const val NOTIFICATION_ID = 3000
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Geofencing event received")

        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent == null) {
            Log.e(TAG, "Geofencing event is null")
            return
        }

        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofencing error: ${geofencingEvent.errorCode}")
            return
        }

        // Récupérer le type de transition (ENTER ou EXIT)
        val geofenceTransition = geofencingEvent.geofenceTransition

        // Récupérer les geofences déclenchées
        val triggeringGeofences = geofencingEvent.triggeringGeofences

        when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                Log.d(TAG, "Entered geofence")
                triggeringGeofences?.forEach { geofence ->
                    onEnterWorkplace(context, geofence.requestId)
                }
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                Log.d(TAG, "Exited geofence")
                triggeringGeofences?.forEach { geofence ->
                    onExitWorkplace(context, geofence.requestId)
                }
            }
            else -> {
                Log.w(TAG, "Unknown geofence transition: $geofenceTransition")
            }
        }
    }

    private fun onEnterWorkplace(context: Context, workplaceName: String) {
        Log.d(TAG, "User entered workplace: $workplaceName")

        // Démarrer automatiquement le pointage avec délai d'annulation
        startAutoPointageWithDelay(context, isStarting = true, workplaceName)
    }

    private fun onExitWorkplace(context: Context, workplaceName: String) {
        Log.d(TAG, "User exited workplace: $workplaceName")

        // Terminer automatiquement le pointage avec délai d'annulation
        startAutoPointageWithDelay(context, isStarting = false, workplaceName)
    }

    private fun startAutoPointageWithDelay(context: Context, isStarting: Boolean, workplaceName: String) {
        // Utiliser un Handler pour démarrer/terminer après 10 secondes
        val intent = Intent(context, AutoPointageService::class.java).apply {
            putExtra("IS_STARTING", isStarting)
            putExtra("WORKPLACE_NAME", workplaceName)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun showNotification(
        context: Context,
        title: String,
        message: String,
        actionText: String,
        actionIntent: PendingIntent
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Créer le canal de notification (requis pour Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Détection automatique GPS",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications de détection automatique du lieu de travail"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent pour ouvrir l'app quand on clique sur la notification
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Construire la notification
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(0, actionText, actionIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createStartWorkIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "START_WORK"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createStopWorkIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "STOP_WORK"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
