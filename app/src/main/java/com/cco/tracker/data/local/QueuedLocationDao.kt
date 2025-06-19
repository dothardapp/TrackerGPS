package com.cco.tracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.cco.tracker.data.model.QueuedLocation

@Dao
interface QueuedLocationDao {
    @Insert
    suspend fun insert(location: QueuedLocation)

    @Query("SELECT * FROM queued_locations ORDER BY timestamp ASC")
    suspend fun getAll(): List<QueuedLocation>

    @Query("DELETE FROM queued_locations WHERE id = :id")
    suspend fun deleteById(id: Int)
}