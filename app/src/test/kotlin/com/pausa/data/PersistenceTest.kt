package com.pausa.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pausa.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class PersistenceTest {
    private lateinit var db:PausaDatabase
    private lateinit var context:Context
    @Before fun setup() {
        context=ApplicationProvider.getApplicationContext()
        db=Room.inMemoryDatabaseBuilder(context,PausaDatabase::class.java).allowMainThreadQueries().build()
    }
    @After fun close() {db.close()}
    @Test fun onlyIncompleteLiveSessionsCanBeRecovered() {
        val w=WorkoutPreset.beginner
        for(status in listOf(SessionStatus.COMPLETED,SessionStatus.CANCELLED)) {
            val s=WorkoutSession("id",w,0,1000,status)
            assertTrue(runCatching {Codec.recoverableSession(Codec.session(s,-1))}.isFailure)
        }
        val terminal=WorkoutSession("id",w,0,w.durationMillis,SessionStatus.PAUSED)
        assertTrue(runCatching {Codec.recoverableSession(Codec.session(terminal,-1))}.isFailure)
        val paused=terminal.copy(elapsedMillis=1234)
        assertEquals(paused,Codec.recoverableSession(Codec.session(paused,-1)))
    }
    @Test fun mixedWorkoutSurvivesSerialization() {
        assertEquals(WorkoutPreset.mixed,Codec.workout(JSONObject(Codec.workout(WorkoutPreset.mixed).toString())))
    }
    @Test fun activeSessionRoundTripKeepsExactElapsedTime()=runBlocking {
        val s=WorkoutSession("id",WorkoutPreset.intermediate,123456,47321,SessionStatus.PAUSED)
        db.dao().saveActive(ActiveSessionEntity(payload=Codec.session(s,1)))
        assertEquals(s,Codec.session(db.dao().active()!!.payload))
    }
    @Test fun completionIsAtomicAndIdempotent()=runBlocking {
        val h=HistoryEntity("same","Iniciante",1,"2026-10-05",90000,10)
        db.dao().saveActive(ActiveSessionEntity(payload="checkpoint"))
        db.dao().complete(h);db.dao().complete(h)
        assertEquals(listOf(h),db.dao().history().first())
        assertNull(db.dao().active())
    }
    @Test fun cancelledCheckpointDoesNotGenerateHistory()=runBlocking {
        db.dao().saveActive(ActiveSessionEntity(payload="checkpoint"))
        db.dao().deleteActive()
        assertTrue(db.dao().history().first().isEmpty())
    }
    @Test fun remindersPersistIndependentDaysAndEnabledState()=runBlocking {
        val a=ReminderEntity("a",8,0,1,true)
        val b=ReminderEntity("b",21,30,64,false)
        db.dao().saveReminder(a);db.dao().saveReminder(b)
        assertEquals(a,db.dao().reminder("a"))
        assertEquals(b,db.dao().reminder("b"))
        db.dao().saveReminder(a.copy(enabled=false))
        assertFalse(db.dao().reminder("a")!!.enabled)
        db.dao().deleteReminder("a");assertNull(db.dao().reminder("a"))
    }
    @Test fun feedbackAndDeleteArePersisted()=runBlocking {
        db.dao().insertHistory(HistoryEntity("id","Quick",1,"2026-10-05",40000,20))
        db.dao().feedback("id","Adequado")
        assertEquals("Adequado",db.dao().history().first().single().feedback)
        db.dao().clearHistory();assertTrue(db.dao().history().first().isEmpty())
    }
}
