package com.mapointeuse.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mapointeuse.MaPointeuseApplication
import com.mapointeuse.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Service qui démarre/termine automatiquement le pointage après un délai.
 * Affiche une notification avec possibilité d'annuler pendant 10 secondes.
 */
class AutoPointageService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var countdownJob: Job? = null

    companion object {
        private const val TAG = "AutoPointageService"
        private const val NOTIFICATION_CHANNEL_ID = "auto_pointage_channel"
        private const val NOTIFICATION_ID = 4000
        private const val COUNTDOWN_SECONDS = 10
        const val ACTION_CANCEL = "com.mapointeuse.ACTION_CANCEL_AUTO_POINTAGE"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            Log.d(TAG, "Auto pointage cancelled by user")
            stopSelf()
            return START_NOT_STICKY
        }

        val isStarting = intent?.getBooleanExtra("IS_STARTING", true) ?: true
        val workplaceName = intent?.getStringExtra("WORKPLACE_NAME") ?: "votre lieu de travail"

        // Créer le canal de notification
        createNotificationChannel()

        // Démarrer en foreground avec notification
        startForeground(NOTIFICATION_ID, createNotification(COUNTDOWN_SECONDS, isStarting, workplaceName))

        // Lancer le compte à rebours
        startCountdown(isStarting, workplaceName)

        return START_NOT_STICKY
    }

    private fun startCountdown(isStarting: Boolean, workplaceName: String) {
        countdownJob = serviceScope.launch {
            // Attendre 10 secondes sans mettre à jour la notification (évite le spam)
            delay(COUNTDOWN_SECONDS * 1000L)

            // Après 10 secondes, démarrer/terminer le pointage
            executePointage(isStarting)
            stopSelf()
        }
    }

    private fun executePointage(isStarting: Boolean) {
        Log.d(TAG, "Executing auto pointage: ${if (isStarting) "START" else "PAUSE"}")

        val repository = (application as MaPointeuseApplication).repository

        serviceScope.launch(Dispatchers.IO) {
            try {
                if (isStarting) {
                    // Démarrer le pointage (startWork vérifie déjà s'il y a un pointage actif)
                    try {
                        repository.startWork()
                        showConfirmationNotification("Journée démarrée automatiquement")
                        Log.d(TAG, "Work started automatically")
                    } catch (e: IllegalStateException) {
                        Log.d(TAG, "Work already active, skipping auto start: ${e.message}")
                    }
                } else {
                    // Mettre en pause (startPause vérifie déjà s'il y a un pointage actif)
                    try {
                        repository.startPause()
                        showConfirmationNotification("Pause automatique activée")
                        Log.d(TAG, "Work paused automatically")
                    } catch (e: IllegalStateException) {
                        Log.d(TAG, "Cannot pause, skipping: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing auto pointage", e)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Démarrage automatique",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications de démarrage/arrêt automatique du pointage"
                enableVibration(true)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(secondsLeft: Int, isStarting: Boolean, workplaceName: String): android.app.Notification {
        val title = if (isStarting) "Arrivée détectée" else "Départ détecté"
        val message = if (isStarting) {
            "Démarrage automatique dans $COUNTDOWN_SECONDS secondes..."
        } else {
            "Pause automatique dans $COUNTDOWN_SECONDS secondes..."
        }

        // Intent pour annuler
        val cancelIntent = Intent(this, AutoPointageService::class.java).apply {
            action = ACTION_CANCEL
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            0,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .addAction(0, "Annuler", cancelPendingIntent)
            .build()
    }

    private fun showConfirmationNotification(message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("MaPointeuse")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownJob?.cancel()
    }
}
