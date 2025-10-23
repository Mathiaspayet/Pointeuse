package com.mapointeuse.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "pauses",
    foreignKeys = [
        ForeignKey(
            entity = Pointage::class,
            parentColumns = ["id"],
            childColumns = ["pointageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["pointageId"])]
)
data class Pause(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pointageId: Long,
    val heureDebut: LocalDateTime,
    val heureFin: LocalDateTime? = null,
    val dureeMinutes: Long = 0
)
