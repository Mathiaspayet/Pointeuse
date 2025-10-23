package com.mapointeuse.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "pointages")
data class Pointage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: LocalDate,
    val heureDebut: LocalDateTime,
    val heureFin: LocalDateTime? = null,
    val tempsTravailleMinutes: Long = 0,
    val statut: StatutPointage = StatutPointage.EN_COURS
)

enum class StatutPointage {
    EN_COURS,
    EN_PAUSE,
    TERMINE
}
