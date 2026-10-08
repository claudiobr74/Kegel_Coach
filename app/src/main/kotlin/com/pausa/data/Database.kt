package com.pausa.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "history", indices = [Index("localDate")])
data class HistoryEntity(
    @PrimaryKey val id: String, val workoutName: String, val completedAt: Long,
    val localDate: String, val durationMillis: Long, val contractions: Int, val feedback: String? = null,
    val programWeek: Int = -1
)
@Entity(tableName = "reminders")
data class ReminderEntity(@PrimaryKey val id: String, val hour: Int, val minute: Int, val daysMask: Int = 127, val enabled: Boolean = true, @ColumnInfo(defaultValue = "0") val snoozedUntil: Long = 0)
@Entity(tableName = "saved_workouts")
data class SavedWorkoutEntity(@PrimaryKey val id: String, val name: String, val payload: String)

@Entity(tableName = "active_session")
data class ActiveSessionEntity(@PrimaryKey val key: Int = 1, val payload: String)

@Dao
interface PausaDao {
    @Query("SELECT * FROM history ORDER BY completedAt DESC") fun history(): Flow<List<HistoryEntity>>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertHistory(entry: HistoryEntity): Long
    @Query("UPDATE history SET feedback=:feedback WHERE id=:id") suspend fun feedback(id: String, feedback: String)
    @Query("SELECT * FROM history ORDER BY completedAt DESC") suspend fun historyOnce(): List<HistoryEntity>
    @Query("SELECT * FROM saved_workouts ORDER BY name COLLATE NOCASE") fun savedWorkouts(): Flow<List<SavedWorkoutEntity>>
    @Query("SELECT * FROM saved_workouts ORDER BY name") suspend fun savedWorkoutsOnce(): List<SavedWorkoutEntity>
    @Upsert suspend fun saveWorkout(workout: SavedWorkoutEntity)
    @Query("DELETE FROM saved_workouts WHERE id=:id") suspend fun deleteWorkout(id: String)
    @Query("DELETE FROM saved_workouts") suspend fun clearWorkouts()
    @Query("DELETE FROM reminders") suspend fun clearReminders()
    @Query("UPDATE reminders SET snoozedUntil=:until WHERE id=:id") suspend fun setSnooze(id: String, until: Long)
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

@Database(entities = [HistoryEntity::class, ReminderEntity::class, ActiveSessionEntity::class, SavedWorkoutEntity::class], version = 2, exportSchema = true)
abstract class PausaDatabase : RoomDatabase() {
    abstract fun dao(): PausaDao
    companion object {
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN snoozedUntil INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_history_localDate ON history(localDate)")
                db.execSQL("CREATE TABLE IF NOT EXISTS saved_workouts (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, payload TEXT NOT NULL)")
            }
        }
        @Volatile private var instance: PausaDatabase? = null
        fun get(context: Context): PausaDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, PausaDatabase::class.java, "pausa.db").addMigrations(MIGRATION_1_2).build().also { instance = it }
        }
    }
}
