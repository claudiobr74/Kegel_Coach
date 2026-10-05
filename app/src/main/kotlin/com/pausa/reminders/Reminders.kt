package com.pausa.reminders

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import androidx.core.net.toUri
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.pausa.MainActivity
import com.pausa.R
import com.pausa.data.*
import com.pausa.domain.Reminder
import kotlinx.coroutines.*
import java.time.*

fun ReminderEntity.domain()=Reminder(id,hour,minute,DayOfWeek.entries.filter { daysMask and (1 shl (it.value-1)) != 0 }.toSet(),enabled)

class ReminderScheduler(private val context:Context) {
    private val alarm=context.getSystemService(AlarmManager::class.java)
    private fun pending(r:ReminderEntity,snooze:Boolean=false,minutes:Int=0):PendingIntent =
        PendingIntent.getBroadcast(context,0,Intent(context,ReminderReceiver::class.java)
            .setAction(if(minutes>0)SNOOZE else FIRE)
            .setData("pausa://reminder/${r.id}/${if(snooze)"snoozed" else if(minutes>0)"delay-$minutes" else "daily"}".toUri())
            .putExtra("id",r.id).putExtra("snoozed",snooze).putExtra("minutes",minutes),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    fun schedule(r:ReminderEntity) {
        alarm.cancel(pending(r))
        if(r.enabled) alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
            r.domain().nextAfter(ZonedDateTime.now()).toInstant().toEpochMilli(),pending(r))
    }
    fun cancel(r:ReminderEntity) {alarm.cancel(pending(r));alarm.cancel(pending(r,true))}
    fun snooze(r:ReminderEntity,minutes:Int) {
        require(minutes in listOf(10,30,60))
        alarm.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,android.os.SystemClock.elapsedRealtime()+minutes*60_000L,pending(r,true))
    }
    fun notify(r:ReminderEntity) {
        if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return
        val manager=context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("reminders","Pausas diárias",NotificationManager.IMPORTANCE_DEFAULT).apply {lockscreenVisibility=Notification.VISIBILITY_PRIVATE})
        val start=PendingIntent.getActivity(context,0,Intent(context,MainActivity::class.java).setAction("reminder-start"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val expanded=RemoteViews(context.packageName,R.layout.reminder_notification)
        expanded.setOnClickPendingIntent(R.id.start,start)
        expanded.setOnClickPendingIntent(R.id.snooze10,pending(r,minutes=10))
        expanded.setOnClickPendingIntent(R.id.snooze30,pending(r,minutes=30))
        expanded.setOnClickPendingIntent(R.id.snooze60,pending(r,minutes=60))
        val notification=NotificationCompat.Builder(context,"reminders").setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Pausa").setContentText("Hora de uma pausa rápida").setContentIntent(start)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setAutoCancel(true)
            .setPublicVersion(NotificationCompat.Builder(context,"reminders").setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Pausa").setContentText("Hora de uma pausa rápida").build())
            .setStyle(NotificationCompat.DecoratedCustomViewStyle()).setCustomBigContentView(expanded)
            .addAction(0,"Iniciar",start).addAction(0,"Adiar 10 min",pending(r,minutes=10)).build()
        manager.notify(r.id,200,notification)
    }
    companion object {const val FIRE="com.pausa.reminder.FIRE";const val SNOOZE="com.pausa.reminder.SNOOZE"}
}

class ReminderReceiver:BroadcastReceiver() {
    override fun onReceive(context:Context,intent:Intent) {
        if(intent.action !in listOf(ReminderScheduler.FIRE,ReminderScheduler.SNOOZE))return
        val result=goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val r=PausaDatabase.get(context).dao().reminder(intent.getStringExtra("id") ?: return@launch) ?: return@launch
                if(!r.enabled)return@launch
                val scheduler=ReminderScheduler(context)
                if(intent.action==ReminderScheduler.SNOOZE) {
                    val minutes=intent.getIntExtra("minutes",0)
                    if(minutes in listOf(10,30,60))scheduler.snooze(r,minutes)
                    context.getSystemService(NotificationManager::class.java).cancel(r.id,200)
                } else {
                    val snoozed=intent.getBooleanExtra("snoozed",false)
                    if(snoozed || LocalDate.now().dayOfWeek in r.domain().days)scheduler.notify(r)
                    if(!snoozed)scheduler.schedule(r)
                }
            } finally {result.finish()}
        }
    }
}
class RescheduleReceiver:BroadcastReceiver() {
    override fun onReceive(context:Context,intent:Intent) {
        if(intent.action !in listOf(Intent.ACTION_BOOT_COMPLETED,Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_TIME_CHANGED,Intent.ACTION_MY_PACKAGE_REPLACED))return
        val result=goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {PausaDatabase.get(context).dao().remindersOnce().forEach {ReminderScheduler(context).schedule(it)}}
            finally {result.finish()}
        }
    }
}
