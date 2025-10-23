package com.mapointeuse.service

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import com.mapointeuse.data.AppDatabase
import com.mapointeuse.data.PointageRepository
import com.mapointeuse.data.WorkPlace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate

class PointageService : Service(), LocationListener {

    private val binder = PointageBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var repository: PointageRepository
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var locationManager: LocationManager
    private lateinit var geofencingManager: GeofencingManager

    private var currentLocation: Location? = null
    private var isTracking = false
    private var activeWorkPlace: WorkPlace? = null

    companion object {
        private const val TAG = "PointageService"
        const val ACTION_START_TRACKING = "com.mapointeuse.START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.mapointeuse.STOP_TRACKING"
        const val ACTION_UPDATE_PAUSE = "com.mapointeuse.UPDATE_PAUSE"

        private const val NOTIFICATION_ID = 1001
        private const val LOCATION_UPDATE_INTERVAL = 30000L // 30 secondes
        private const val LOCATION_UPDATE_DISTANCE = 50f // 50 mètres

        fun startTracking(context: Context) {
            val intent = Intent(context, PointageService::class.java).apply {
                action = ACTION_START_TRACKING
            }
            context.startForegroundService(intent)
        }

        fun stopTracking(context: Context) {
            val intent = Intent(context, PointageService::class.java).apply {
                action = ACTION_STOP_TRACKING
            }
            context.startService(intent)
        }

        fun updatePauseStatus(context: Context) {
            val intent = Intent(context, PointageService::class.java).apply {
                action = ACTION_UPDATE_PAUSE
            }
            context.startService(intent)
        }
    }

    inner class PointageBinder : Binder() {
        fun getService(): PointageService = this@PointageService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        notificationHelper = NotificationHelper(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        geofencingManager = GeofencingManager(this)

        // Le repository sera injecté via Application
        val app = application as com.mapointeuse.MaPointeuseApplication
        repository = app.repository
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> startTracking()
            ACTION_STOP_TRACKING -> stopTracking()
            ACTION_UPDATE_PAUSE -> updateNotification()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private fun startTracking() {
        if (isTracking) {
            Log.d(TAG, "Already tracking")
            return
        }

        Log.d(TAG, "Starting tracking")
        isTracking = true

        // Démarrer le service au premier plan avec notification
        val notification = notificationHelper.createTrackingNotification(
            isPaused = false,
            location = null
        )
        startForeground(NOTIFICATION_ID, notification)

        // Charger le lieu de travail actif pour le geofencing
        loadActiveWorkPlace()

        // Démarrer le suivi de localisation
        startLocationUpdates()

        // Observer les changements de pointage pour mettre à jour la notification
        observePointageChanges()
    }

    private fun stopTracking() {
        Log.d(TAG, "Stopping tracking")
        isTracking = false

        // Arrêter le suivi de localisation
        stopLocationUpdates()

        // Arrêter le service au premier plan
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Location permission not granted")
            return
        }

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                LOCATION_UPDATE_INTERVAL,
                LOCATION_UPDATE_DISTANCE,
                this
            )

            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                LOCATION_UPDATE_INTERVAL,
                LOCATION_UPDATE_DISTANCE,
                this
            )

            Log.d(TAG, "Location updates started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting location updates", e)
        }
    }

    private fun stopLocationUpdates() {
        try {
            locationManager.removeUpdates(this)
            Log.d(TAG, "Location updates stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping location updates", e)
        }
    }

    private fun observePointageChanges() {
        serviceScope.launch {
            repository.getPointagesForDate(LocalDate.now()).collect {
                updateNotification()
            }
        }
    }

    private fun updateNotification() {
        if (!isTracking) return

        serviceScope.launch {
            val today = LocalDate.now()
            val activePointage = repository.observeActivePointageForDate(today).firstOrNull()
            val pointage = activePointage ?: repository.getLatestPointageForDate(today).firstOrNull()
            val isPaused = activePointage?.statut == com.mapointeuse.data.StatutPointage.EN_PAUSE

            val notification = notificationHelper.createTrackingNotification(
                isPaused = isPaused,
                location = currentLocation
            )

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as android.app.NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun loadActiveWorkPlace() {
        serviceScope.launch {
            val database = AppDatabase.getDatabase(applicationContext)
            database.workPlaceDao().getActiveWorkPlace().collect { workPlace ->
                activeWorkPlace = workPlace
                if (workPlace != null) {
                    Log.d(TAG, "Active workplace loaded: ${workPlace.name}")
                    // Réinitialiser le gestionnaire de géofencing avec le nouveau lieu
                    geofencingManager.reset()
                } else {
                    Log.d(TAG, "No active workplace configured")
                }
            }
        }
    }

    // LocationListener methods
    override fun onLocationChanged(location: Location) {
        currentLocation = location
        Log.d(TAG, "Location updated: ${location.latitude}, ${location.longitude}")

        // Vérifier la géolocalisation si un lieu de travail est configuré
        activeWorkPlace?.let { workPlace ->
            geofencingManager.checkLocation(location, workPlace)
        }

        updateNotification()
    }

    override fun onProviderEnabled(provider: String) {
        Log.d(TAG, "Provider enabled: $provider")
    }

    override fun onProviderDisabled(provider: String) {
        Log.d(TAG, "Provider disabled: $provider")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        stopLocationUpdates()
        serviceScope.cancel()
    }

    fun getCurrentLocation(): Location? = currentLocation
}
