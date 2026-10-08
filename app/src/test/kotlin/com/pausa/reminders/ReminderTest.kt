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
    @Test fun everyWeekdayMaskSchedulesOnlySelectedDaysInLocalTime() {
        val now=java.time.ZonedDateTime.of(2026,10,7,9,0,0,0,java.time.ZoneId.of("America/Sao_Paulo"))
        for(mask in 1..127) {
            val r=ReminderEntity("mask-$mask",8,30,mask).domain()
            val next=r.nextAfter(now)
            assertTrue(next.isAfter(now))
            assertTrue(mask and (1 shl (next.dayOfWeek.value-1))!=0)
            assertEquals(8,next.hour)
            assertEquals(30,next.minute)
            assertEquals(now.zone,next.zone)
            val earlier=(0..7).map {now.toLocalDate().plusDays(it.toLong()).atTime(8,30).atZone(now.zone)}
                .filter {it.isAfter(now) && mask and (1 shl (it.dayOfWeek.value-1))!=0}.first()
            assertEquals(earlier,next)
        }
    }
    @Test fun disablingOrClearingDaysCancelsDailyAndSnoozedAlarms() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val scheduler=ReminderScheduler(context)
        val r=ReminderEntity("cancel-all",8,30,127)
        val alarms=shadowOf(context.getSystemService(AlarmManager::class.java))
        scheduler.schedule(r);scheduler.snooze(r,10)
        scheduler.schedule(r.copy(enabled=false))
        assertTrue(alarms.scheduledAlarms.isEmpty())
        scheduler.schedule(r);scheduler.snooze(r,30)
        scheduler.schedule(r.copy(daysMask=0))
        assertTrue(alarms.scheduledAlarms.isEmpty())
    }
    @Test fun eightOClockSchedulesAtEightInTheDeviceTimeZone() {
        val previous=java.util.TimeZone.getDefault()
        try {
            val context=ApplicationProvider.getApplicationContext<Context>()
            val scheduler=ReminderScheduler(context)
            val alarms=shadowOf(context.getSystemService(AlarmManager::class.java))
            for(zone in listOf("America/Sao_Paulo","America/Manaus","Europe/Berlin")) {
                java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(zone))
                val r=ReminderEntity("eight-$zone",8,0,127)
                scheduler.schedule(r)
                val fired=java.time.Instant.ofEpochMilli(alarms.scheduledAlarms.single().triggerAtMs)
                    .atZone(java.time.ZoneId.of(zone))
                assertEquals(8,fired.hour)
                assertEquals(0,fired.minute)
                if(zone=="America/Sao_Paulo")assertEquals(11,fired.withZoneSameInstant(java.time.ZoneOffset.UTC).hour)
                scheduler.cancel(r)
            }
        } finally {java.util.TimeZone.setDefault(previous)}
    }
}
