package com.pausa.data

import androidx.test.core.app.ApplicationProvider
import com.pausa.domain.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class BackupTest {
    private fun snapshot():BackupSnapshot {
        val w=Workout("saved-test","Meu misto",WorkoutPreset.mixed.blocks,2,30)
        return BackupSnapshot(Settings(UserPreferences(true,Guidance.VOICE,true,AppTheme.DARK,true,true),w,2,123L),
            listOf(HistoryEntity("h","Iniciante",123L,"2026-10-08",90000L,10,"Adequado",1)),
            listOf(ReminderEntity("r",8,0,31)),listOf(SavedWorkoutEntity(w.id,w.name,Codec.workout(w).toString())))
    }
    @Test fun encryptedCopyPreservesSettingsHistoryDaysAndMixedWorkout() {
        val original=snapshot();val bytes=BackupCodec.encrypt(original,"senha-forte-123")
        assertFalse(String(bytes,Charsets.ISO_8859_1).contains("Meu misto"))
        assertEquals(original,BackupCodec.decrypt(bytes,"senha-forte-123"))
        assertFalse(bytes.contentEquals(BackupCodec.encrypt(original,"senha-forte-123")))
    }
    @Test fun wrongPasswordTamperingAndShortPasswordAreRejected() {
        val bytes=BackupCodec.encrypt(snapshot(),"senha-forte-123")
        assertThrows(IllegalArgumentException::class.java) {BackupCodec.decrypt(bytes,"senha-errada-123")}
        bytes[bytes.lastIndex]=(bytes.last().toInt() xor 1).toByte()
        assertThrows(IllegalArgumentException::class.java) {BackupCodec.decrypt(bytes,"senha-forte-123")}
        assertThrows(IllegalArgumentException::class.java) {BackupCodec.encrypt(snapshot(),"123")}
        assertThrows(IllegalArgumentException::class.java) {BackupCodec.decrypt(ByteArray(10),"senha-forte-123")}
    }
    @Test fun duplicateIdsAndInvalidDaysAreRejectedBeforeRestore() {
        val source=snapshot()
        assertThrows(IllegalArgumentException::class.java) {BackupCodec.encrypt(source.copy(history=source.history+source.history),"senha-forte-123")}
        assertThrows(IllegalArgumentException::class.java) {BackupCodec.encrypt(source.copy(reminders=listOf(ReminderEntity("r",8,0,0))),"senha-forte-123")}
    }
}
