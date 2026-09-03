package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SafetyOccurrence
import kotlinx.coroutines.flow.Flow

@Dao
interface OccurrenceDao {
    @Query("SELECT * FROM occurrences ORDER BY id DESC")
    fun getAllOccurrences(): Flow<List<SafetyOccurrence>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOccurrence(occurrence: SafetyOccurrence): Long

    @Query("SELECT * FROM occurrences WHERE sincronizadoGooglePlanilhas = 0 ORDER BY id ASC")
    suspend fun getUnsyncedOccurrences(): List<SafetyOccurrence>

    @Query("UPDATE occurrences SET sincronizadoGooglePlanilhas = :synced WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, synced: Boolean)

    @Delete
    suspend fun deleteOccurrence(occurrence: SafetyOccurrence)

    @Query("DELETE FROM occurrences")
    suspend fun deleteAllOccurrences()

    @Query("DELETE FROM sqlite_sequence WHERE name = 'occurrences'")
    suspend fun resetOccurrencesSequence()

    @androidx.room.Transaction
    suspend fun clearAllOccurrences() {
        deleteAllOccurrences()
        try {
            resetOccurrencesSequence()
        } catch (_: Exception) {
            // Ignored if sqlite_sequence does not exist or table not initialized
        }
    }
}
