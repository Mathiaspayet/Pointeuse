package com.mapointeuse

import android.app.Application
import com.mapointeuse.data.AppDatabase
import com.mapointeuse.data.PointageRepository

class MaPointeuseApplication : Application() {

    // Initialisation lazy du repository pour qu'il soit accessible partout dans l'app
    val repository: PointageRepository by lazy {
        val database = AppDatabase.getDatabase(applicationContext)
        PointageRepository(database.pointageDao())
    }

    override fun onCreate() {
        super.onCreate()
        // Initialisation de l'application si nécessaire
    }
}
