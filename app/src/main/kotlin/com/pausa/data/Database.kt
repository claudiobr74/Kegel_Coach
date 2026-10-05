package com.pausa.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val id: String, val workoutName: String, val completedAt: Long,
    val localDate: String, val durationMillis: Long, val contractions: Int, val feedback: String? = null,
    val programWeek: Int = -1
)
@Entity(tableName = "reminders")
data class ReminderEntity(@PrimaryKey val id: String, val hour: Int, val minute: Int, val daysMask: Int = 127, val enabled: Boolean = true)
@Entity(tableName = "active_session")
data class ActiveSessionEntity(@PrimaryKey val key: Int = 1, val payload: String)

@Dao
interface PausaDao {
    @Query("SELECT * FROM history ORDER BY completedAt DESC") fun history(): Flow<List<HistoryEntity>>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertHistory(entry: HistoryEntity): Long
    @Query("UPDATE history SET feedback=:feedback WHERE id=:id") suspend fun feedback(id: String, feedback: String)
    @Query("DELETE FROM history") suspend fun clearHistory()
    @Query("SELECT * FROM reminders ORDER BY hour, minute") fun reminders(): Flow<List<ReminderEntity>>
    @Query("SELECT * FROM reminders") suspend fun remindersOnce(): List<ReminderEntity>
    @Query("SELECT * FROM reminders WHERE id=:id") suspend fun reminder(id: String): ReminderEntity?
    @Upsert suspend fun saveReminder(reminder: ReminderEntity)
    @Query("DELETE FROM reminders WHERE id=:id") suspend fun deleteReminder(id: String)
    @Query("SELECT * FROM active_session WHERE key=1") suspend fun active(): ActiveSessionEntity?
    @Upsert suspend fun saveActive(active: ActiveSessionEntity)
    @Query("DELETE FROM active_session") suspend fun deleteActive()
    @Transaction suspend fun complete(entry: HistoryEntity) { insertHistory(entry); deleteActive() }
}

@Database(entities = [HistoryEntity::class, ReminderEntity::class, ActiveSessionEntity::class], version = 1, exportSchema = true)
abstract class PausaDatabase : RoomDatabase() {
    abstract fun dao(): PausaDao
    companion object {
        @Volatile private var instance: PausaDatabase? = null
        fun get(context: Context): PausaDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, PausaDatabase::class.java, "pausa.db").build().also { instance = it }
        }
    }
}
