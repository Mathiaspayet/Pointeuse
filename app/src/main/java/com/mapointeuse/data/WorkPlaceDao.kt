package com.mapointeuse.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkPlaceDao {
    @Query("SELECT * FROM workplaces WHERE isActive = 1 LIMIT 1")
    fun getActiveWorkPlace(): Flow<WorkPlace?>

    @Query("SELECT * FROM workplaces WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveWorkPlaceSync(): WorkPlace?

    @Query("SELECT * FROM workplaces")
    fun getAllWorkPlaces(): Flow<List<WorkPlace>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workPlace: WorkPlace): Long

    @Update
    suspend fun update(workPlace: WorkPlace)

    @Delete
    suspend fun delete(workPlace: WorkPlace)

    @Query("UPDATE workplaces SET isActive = 0 WHERE id != :workPlaceId")
    suspend fun deactivateOthers(workPlaceId: Long)

    @Query("DELETE FROM workplaces")
    suspend fun deleteAll()
}
