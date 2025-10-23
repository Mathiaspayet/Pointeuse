package com.mapointeuse.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workplaces")
data class WorkPlace(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int = 100, // Rayon de détection par défaut: 100m
    val isActive: Boolean = true,
    val autoStart: Boolean = false, // Démarrage automatique
    val autoStop: Boolean = false,  // Arrêt automatique
    val notifyOnEnter: Boolean = true, // Notification à l'entrée
    val notifyOnExit: Boolean = true   // Notification à la sortie
)
