package com.pausa.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.*
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class MigrationTest {
    @Test fun existingVersionOneDataSurvivesMigrationAndAcceptsNewLibrary()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>();val name="migration-test.db"
        context.deleteDatabase(name)
        val old=FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
            .callback(object:SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db:SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE history (id TEXT NOT NULL PRIMARY KEY, workoutName TEXT NOT NULL, completedAt INTEGER NOT NULL, localDate TEXT NOT NULL, durationMillis INTEGER NOT NULL, contractions INTEGER NOT NULL, feedback TEXT, programWeek INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE reminders (id TEXT NOT NULL PRIMARY KEY, hour INTEGER NOT NULL, minute INTEGER NOT NULL, daysMask INTEGER NOT NULL, enabled INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE active_session (`key` INTEGER NOT NULL PRIMARY KEY, payload TEXT NOT NULL)")
                    db.execSQL("INSERT INTO history VALUES ('old','Iniciante',1,'2026-10-07',90000,10,'Adequado',0)")
                    db.execSQL("INSERT INTO reminders VALUES ('r',8,0,96,1)")
                }
                override fun onUpgrade(db:SupportSQLiteDatabase,oldVersion:Int,newVersion:Int) {}
            }).build())
        old.writableDatabase;old.close()
        val db=Room.databaseBuilder(context,PausaDatabase::class.java,name).addMigrations(PausaDatabase.MIGRATION_1_2).build()
        try {
            assertEquals("old",db.dao().historyOnce().single().id)
            assertEquals(96,db.dao().remindersOnce().single().daysMask)
            assertEquals(0L,db.dao().remindersOnce().single().snoozedUntil)
            db.dao().saveWorkout(SavedWorkoutEntity("s","Salvo","{}"))
            assertEquals("s",db.dao().savedWorkoutsOnce().single().id)
        } finally {db.close();context.deleteDatabase(name)}
    }
}
