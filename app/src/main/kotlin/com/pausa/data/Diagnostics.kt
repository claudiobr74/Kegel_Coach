package com.pausa.data

import android.content.Context
import androidx.core.content.edit
import java.time.Instant

/** Only technical events; never workout names, symptoms or user-entered text. */
object Diagnostics {
    @Synchronized fun record(context: Context, event: String) {
        val prefs=context.getSharedPreferences("diagnostics",Context.MODE_PRIVATE)
        val lines=prefs.getString("events","").orEmpty().lines().filter {it.isNotBlank()}
        prefs.edit {putString("events",(lines+"${Instant.now()} · $event").takeLast(30).joinToString("\n"))}
    }
    fun read(context: Context): String = context.getSharedPreferences("diagnostics",Context.MODE_PRIVATE)
        .getString("events","").orEmpty().ifBlank {"Nenhum evento técnico registrado."}
}
