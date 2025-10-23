package com.mapointeuse.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mapointeuse.MainActivity
import com.mapointeuse.R
import com.mapointeuse.data.WorkPlace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GeofencingManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isInsideWorkPlace = false
    private var lastNotificationTime = 0L

    companion object {
        private const val TAG = "GeofencingManager"
        private const val CHANNEL_ID = "geofencing_channel"
        private const val NOTIFICATION_ID_ENTER = 2001
        private const val NOTIFICATION_ID_EXIT = 2002
        private const val MIN_NOTIFICATION_INTERVAL = 300000L // 5 minutes minimum entre notifications
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Détection automatique"
            val descriptionText = "Notifications pour le démarrage/arrêt automatique du pointage"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Vérifie si la position actuelle est dans la zone de travail
     */
    fun checkLocation(currentLocation: Location, workPlace: WorkPlace) {
        val workPlaceLocation = Location("").apply {
            latitude = workPlace.latitude
            longitude = workPlace.longitude
        }

        val distance = currentLocation.distanceTo(workPlaceLocation)
        val isInside = distance <= workPlace.radiusMeters

        Log.d(TAG, "Distance to workplace: ${distance}m (threshold: ${workPlace.radiusMeters}m)")

        // Détection d'entrée dans la zone
        if (isInside && !isInsideWorkPlace) {
            isInsideWorkPlace = true
            onEnterWorkPlace(workPlace)
        }
        // Détection de sortie de la zone
        else if (!isInside && isInsideWorkPlace) {
            isInsideWorkPlace = false
            onExitWorkPlace(workPlace)
        }
    }

    private fun onEnterWorkPlace(workPlace: WorkPlace) {
        Log.d(TAG, "Entered workplace: ${workPlace.name}")

        // Éviter les notifications trop fréquentes
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastNotificationTime < MIN_NOTIFICATION_INTERVAL) {
            Log.d(TAG, "Skipping notification (too soon)")
            return
        }
        lastNotificationTime = currentTime

        if (workPlace.notifyOnEnter) {
            showEnterNotification(workPlace)
        }

        if (workPlace.autoStart) {
            // TODO: Déclencher automatiquement le pointage
            Log.d(TAG, "Auto-start enabled - would start pointing now")
        }
    }

    private fun onExitWorkPlace(workPlace: WorkPlace) {
        Log.d(TAG, "Exited workplace: ${workPlace.name}")

        // Éviter les notifications trop fréquentes
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastNotificationTime < MIN_NOTIFICATION_INTERVAL) {
            Log.d(TAG, "Skipping notification (too soon)")
            return
        }
        lastNotificationTime = currentTime

        if (workPlace.notifyOnExit) {
            showExitNotification(workPlace)
        }

        if (workPlace.autoStop) {
            // TODO: Arrêter automatiquement le pointage
            Log.d(TAG, "Auto-stop enabled - would stop pointing now")
        }
    }

    private fun showEnterNotification(workPlace: WorkPlace) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Action pour démarrer le pointage
        val startIntent = Intent(context, MainActivity::class.java).apply {
            action = "START_WORK"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val startPendingIntent = PendingIntent.getActivity(
            context, 1, startIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Arrivée au travail détectée")
            .setContentText("Vous êtes arrivé à ${workPlace.name}. Voulez-vous commencer votre journée ?")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_play, "Commencer", startPendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_ENTER, notification)
    }

    private fun showExitNotification(workPlace: WorkPlace) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Action pour arrêter le pointage
        val stopIntent = Intent(context, MainActivity::class.java).apply {
            action = "STOP_WORK"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val stopPendingIntent = PendingIntent.getActivity(
            context, 2, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Départ du travail détecté")
            .setContentText("Vous avez quitté ${workPlace.name}. Voulez-vous terminer votre journée ?")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Terminer", stopPendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_EXIT, notification)
    }

    fun reset() {
        isInsideWorkPlace = false
        lastNotificationTime = 0L
    }
}
