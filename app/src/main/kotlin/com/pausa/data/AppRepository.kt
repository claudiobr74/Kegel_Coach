package com.pausa.data

import android.content.Context
import androidx.room.withTransaction
import com.pausa.reminders.ReminderScheduler
import kotlinx.coroutines.flow.first

class AppRepository(context:Context) {
    private val db=PausaDatabase.get(context)
    val dao=db.dao()
    val preferences=PreferencesRepository(context)
    suspend fun snapshot():BackupSnapshot=BackupSnapshot(preferences.settings.first(),dao.historyOnce(),dao.remindersOnce(),dao.savedWorkoutsOnce())
    suspend fun restore(snapshot:BackupSnapshot) {
        check(dao.active()==null) {"Encerre o treino ativo antes de restaurar uma cópia."}
        val previous=preferences.settings.first()
        try {
            db.withTransaction {
                dao.clearHistory();dao.clearReminders();dao.clearWorkouts()
                snapshot.history.forEach {dao.insertHistory(it)}
                snapshot.reminders.forEach {dao.saveReminder(it)}
                snapshot.workouts.forEach {dao.saveWorkout(it)}
                preferences.restore(snapshot.settings)
            }
        } catch(e:Exception) {runCatching {preferences.restore(previous)};throw e}
    }
}
