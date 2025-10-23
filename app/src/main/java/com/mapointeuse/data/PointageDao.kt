package com.mapointeuse.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface PointageDao {
    @Query("SELECT * FROM pointages ORDER BY date DESC, heureDebut DESC")
    fun getAllPointages(): Flow<List<Pointage>>

    @Query("SELECT * FROM pointages WHERE date = :date ORDER BY heureDebut DESC")
    fun getPointagesForDate(date: LocalDate): Flow<List<Pointage>>

    @Query("SELECT * FROM pointages WHERE date = :date ORDER BY heureDebut DESC LIMIT 1")
    fun getLatestPointageForDate(date: LocalDate): Flow<Pointage?>

    @Query("SELECT * FROM pointages WHERE date = :date ORDER BY heureDebut DESC LIMIT 1")
    suspend fun getLatestPointageForDateSync(date: LocalDate): Pointage?

    @Query("SELECT * FROM pointages WHERE date = :date AND statut != :terminated ORDER BY heureDebut DESC LIMIT 1")
    fun getActivePointageForDate(date: LocalDate, terminated: StatutPointage): Flow<Pointage?>

    @Query("SELECT * FROM pointages WHERE date = :date AND statut != :terminated ORDER BY heureDebut DESC LIMIT 1")
    suspend fun getActivePointageForDateSync(date: LocalDate, terminated: StatutPointage): Pointage?

    @Query("SELECT * FROM pointages WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC, heureDebut DESC")
    fun getPointagesBetweenDates(startDate: LocalDate, endDate: LocalDate): Flow<List<Pointage>>

    @Insert
    suspend fun insert(pointage: Pointage): Long

    @Update
    suspend fun update(pointage: Pointage)

    @Delete
    suspend fun delete(pointage: Pointage)

    @Query("SELECT * FROM pointages WHERE id = :id")
    suspend fun getPointageById(id: Long): Pointage?
}
