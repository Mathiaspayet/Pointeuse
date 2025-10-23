package com.mapointeuse

import android.app.Application
import com.mapointeuse.data.AppDatabase
import com.mapointeuse.data.PointageRepository

class MaPointeuseApplication : Application() {

    // Initialisation eager de la database et du repository
    lateinit var repository: PointageRepository
        private set

    override fun onCreate() {
        super.onCreate()
        // Initialiser la database dès le démarrage de l'app
        val database = AppDatabase.getDatabase(applicationContext)
        repository = PointageRepository(database.pointageDao())
    }
}
