package com.pausa.reminders

import android.app.AlarmManager
import android.content.Context
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import com.pausa.data.ReminderEntity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class ReminderTest {
    @Test fun reschedulingDoesNotDuplicateDailyAlarmOrLoseExplicitSnooze() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val scheduler=ReminderScheduler(context)
        val r=ReminderEntity("isolated",8,30,127)
        val alarms=shadowOf(context.getSystemService(AlarmManager::class.java))
        scheduler.schedule(r);scheduler.schedule(r)
        assertEquals(1,alarms.scheduledAlarms.size)
        val now=SystemClock.elapsedRealtime()
        scheduler.snooze(r,10);scheduler.schedule(r)
        assertEquals(2,alarms.scheduledAlarms.size)
        val snoozed=alarms.scheduledAlarms.single {it.getType()==AlarmManager.ELAPSED_REALTIME_WAKEUP}
        assertEquals(now+600000,snoozed.triggerAtMs)
        scheduler.cancel(r);assertTrue(alarms.scheduledAlarms.isEmpty())
    }
    @Test fun disabledReminderAndDeletionLeaveNoPendingAlarms() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val scheduler=ReminderScheduler(context)
        val r=ReminderEntity("disabled",21,0,1)
        val alarms=shadowOf(context.getSystemService(AlarmManager::class.java))
        scheduler.schedule(r.copy(enabled=false));assertTrue(alarms.scheduledAlarms.isEmpty())
        scheduler.schedule(r);scheduler.snooze(r,30);scheduler.cancel(r)
        assertTrue(alarms.scheduledAlarms.isEmpty())
    }
    @Test(expected=IllegalArgumentException::class) fun unsupportedDelayIsRejected() {
        ReminderScheduler(ApplicationProvider.getApplicationContext()).snooze(ReminderEntity("r",8,0),5)
    }
}
