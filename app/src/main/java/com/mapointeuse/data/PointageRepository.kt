package com.mapointeuse.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class PointageRepository(private val pointageDao: PointageDao) {

    fun getAllPointages(): Flow<List<Pointage>> = pointageDao.getAllPointages()

    fun getPointagesForDate(date: LocalDate): Flow<List<Pointage>> =
        pointageDao.getPointagesForDate(date)

    fun getLatestPointageForDate(date: LocalDate): Flow<Pointage?> =
        pointageDao.getLatestPointageForDate(date)

    fun observeActivePointageForDate(date: LocalDate): Flow<Pointage?> =
        pointageDao.getActivePointageForDate(date, StatutPointage.TERMINE)

    fun getPointagesBetweenDates(startDate: LocalDate, endDate: LocalDate): Flow<List<Pointage>> =
        pointageDao.getPointagesBetweenDates(startDate, endDate)

    suspend fun startWork(): Long {
        val now = LocalDateTime.now()
        val today = LocalDate.now()

        val activePointage = pointageDao.getActivePointageForDateSync(today, StatutPointage.TERMINE)
        if (activePointage != null) {
            throw IllegalStateException("Un pointage est déjà en cours pour aujourd'hui")
        }

        val pointage = Pointage(
            date = today,
            heureDebut = now,
            statut = StatutPointage.EN_COURS
        )
        return pointageDao.insert(pointage)
    }

    suspend fun endWork() {
        val today = LocalDate.now()
        val pointage = pointageDao.getActivePointageForDateSync(today, StatutPointage.TERMINE)
            ?: throw IllegalStateException("Aucun pointage en cours pour aujourd'hui")

        val now = LocalDateTime.now()
        val tempsTravaille = ChronoUnit.MINUTES.between(pointage.heureDebut, now)

        val updatedPointage = pointage.copy(
            heureFin = now,
            tempsTravailleMinutes = tempsTravaille,
            statut = StatutPointage.TERMINE
        )
        pointageDao.update(updatedPointage)
    }

    suspend fun startPause() {
        val today = LocalDate.now()
        val pointage = pointageDao.getActivePointageForDateSync(today, StatutPointage.TERMINE)
            ?: throw IllegalStateException("Aucun pointage en cours pour aujourd'hui")

        if (pointage.statut != StatutPointage.EN_COURS) {
            throw IllegalStateException("Le pointage doit être en cours pour démarrer une pause")
        }

        val updatedPointage = pointage.copy(statut = StatutPointage.EN_PAUSE)
        pointageDao.update(updatedPointage)
    }

    suspend fun endPause() {
        val today = LocalDate.now()
        val pointage = pointageDao.getActivePointageForDateSync(today, StatutPointage.TERMINE)
            ?: throw IllegalStateException("Aucun pointage en cours pour aujourd'hui")

        if (pointage.statut != StatutPointage.EN_PAUSE) {
            throw IllegalStateException("Aucune pause en cours")
        }

        val updatedPointage = pointage.copy(statut = StatutPointage.EN_COURS)
        pointageDao.update(updatedPointage)
    }

    suspend fun deletePointage(pointage: Pointage) {
        pointageDao.delete(pointage)
    }

    suspend fun getPointageById(id: Long): Pointage? =
        pointageDao.getPointageById(id)

    suspend fun updatePointage(pointage: Pointage) {
        val tempsTravaille = if (pointage.heureFin != null) {
            ChronoUnit.MINUTES.between(pointage.heureDebut, pointage.heureFin)
        } else {
            0L
        }
        val updatedPointage = pointage.copy(tempsTravailleMinutes = tempsTravaille)
        pointageDao.update(updatedPointage)
    }
}
