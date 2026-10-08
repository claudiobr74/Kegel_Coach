package com.pausa.data

import com.pausa.domain.*
import org.json.*
import java.time.LocalDate
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Portable user-requested copies. Active sessions and diagnostics are deliberately excluded. */
data class BackupSnapshot(val settings:Settings,val history:List<HistoryEntity>,
    val reminders:List<ReminderEntity>,val workouts:List<SavedWorkoutEntity>)
object BackupCodec {
    const val MAX_BYTES=4*1024*1024
    private val marker=byteArrayOf(75,67,66,49)
    private fun key(password:String,salt:ByteArray):SecretKeySpec {
        require(password.length in 8..256) {"Use uma senha com pelo menos 8 caracteres."}
        val spec=PBEKeySpec(password.toCharArray(),salt,210000,256)
        return try {SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded,"AES")} finally {spec.clearPassword()}
    }
    fun encrypt(snapshot:BackupSnapshot,password:String):ByteArray {
        val salt=ByteArray(16);val iv=ByteArray(12);SecureRandom().apply {nextBytes(salt);nextBytes(iv)}
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE,key(password,salt),GCMParameterSpec(128,iv))
        cipher.updateAAD(marker)
        val document=json(snapshot)
        parse(document) // Refuse to create a copy that this version cannot restore.
        val plain=document.toString().toByteArray(Charsets.UTF_8)
        require(plain.size<=MAX_BYTES-64) {"A cópia excede o tamanho permitido."}
        return marker+salt+iv+cipher.doFinal(plain)
    }
    fun decrypt(bytes:ByteArray,password:String):BackupSnapshot {
        require(bytes.size in 48..MAX_BYTES && bytes.copyOfRange(0,4).contentEquals(marker)) {"Arquivo de cópia inválido."}
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE,key(password,bytes.copyOfRange(4,20)),GCMParameterSpec(128,bytes.copyOfRange(20,32)))
        cipher.updateAAD(marker)
        val raw=try {cipher.doFinal(bytes.copyOfRange(32,bytes.size))} catch(e:javax.crypto.AEADBadTagException) {
            throw IllegalArgumentException("Senha incorreta ou arquivo danificado.")
        }
        return parse(JSONObject(String(raw,Charsets.UTF_8)))
    }
    private fun json(b:BackupSnapshot)=JSONObject().put("format",1).put("settings",JSONObject().apply {
        put("workout",Codec.workout(b.settings.workout));put("week",b.settings.progressionWeek);put("started",b.settings.progressionStarted)
        val u=b.settings.user;put("guidance",u.guidance.name);put("discreet",u.discreetScreen);put("theme",u.theme.name)
        put("progression",u.progressionEnabled);put("preparation",u.preparationEnabled)
    }).put("history",JSONArray().apply {b.history.forEach {h->put(JSONObject().apply {
        put("id",h.id);put("name",h.workoutName);put("at",h.completedAt);put("date",h.localDate)
        put("duration",h.durationMillis);put("contractions",h.contractions);put("feedback",h.feedback ?: JSONObject.NULL);put("week",h.programWeek)
    })}}).put("reminders",JSONArray().apply {b.reminders.forEach {r->put(JSONObject().apply {
        put("id",r.id);put("hour",r.hour);put("minute",r.minute);put("days",r.daysMask);put("enabled",r.enabled)
    })}}).put("workouts",JSONArray().apply {b.workouts.forEach {w->put(Codec.workout(Codec.workout(JSONObject(w.payload))))}})
    internal fun parse(j:JSONObject):BackupSnapshot {
        require(j.getInt("format")==1) {"Versão de cópia não compatível."}
        fun string(o:JSONObject,k:String,max:Int=128)=o.getString(k).also {require(it.isNotBlank() && it.length<=max)}
        fun workout(o:JSONObject)=Codec.workout(o).also {require(it.id.isNotBlank() && it.id.length<=128 && it.name.isNotBlank() && it.name.length<=80)}
        fun <T> list(key:String,max:Int,read:(JSONObject)->T):List<T> {
            val a=j.getJSONArray(key);require(a.length()<=max)
            return (0 until a.length()).map {read(a.getJSONObject(it))}
        }
        val s=j.getJSONObject("settings");val week=s.getInt("week").also {require(it in 0..3)}
        val started=s.getLong("started").also {require(it>=0)}
        val settings=Settings(UserPreferences(true,Guidance.valueOf(s.getString("guidance")),s.getBoolean("discreet"),
            AppTheme.valueOf(s.getString("theme")),s.getBoolean("progression"),s.optBoolean("preparation",false)),workout(s.getJSONObject("workout")),week,started)
        val history=list("history",20000) {h->
            val date=string(h,"date");LocalDate.parse(date)
            val at=h.getLong("at").also {require(it>0)}
            val duration=h.getLong("duration").also {require(it in 1..100_000_000L)}
            val contractions=h.getInt("contractions").also {require(it in 1..1000)}
            val programWeek=h.getInt("week").also {require(it in -1..3)}
            val feedback=if(h.isNull("feedback"))null else h.getString("feedback").also {require(it in listOf("Muito fácil","Adequado","Difícil","Desconforto"))}
            HistoryEntity(string(h,"id"),string(h,"name",80),at,date,duration,contractions,feedback,programWeek)
        }
        val reminders=list("reminders",100) {r->
            ReminderEntity(string(r,"id"),r.getInt("hour").also {require(it in 0..23)},r.getInt("minute").also {require(it in 0..59)},
                r.getInt("days").also {require(it in 1..127)},r.getBoolean("enabled"))
        }
        val workouts=list("workouts",100) {w->workout(w).let {SavedWorkoutEntity(it.id,it.name,Codec.workout(it).toString())}}
        require(history.map {it.id}.distinct().size==history.size)
        require(reminders.map {it.id}.distinct().size==reminders.size)
        require(workouts.map {it.id}.distinct().size==workouts.size)
        return BackupSnapshot(settings,history,reminders,workouts)
    }
}
