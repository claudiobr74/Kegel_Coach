package com.pausa.presentation

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import kotlin.reflect.KProperty

internal enum class AppRoute { HOME, PROGRAMS, PROGRESS, SETTINGS, CUSTOM, SESSION, FINISHED, MANUAL, REMINDERS, POCKET, DATA, HELP }
internal class AppNavigator(initial:List<AppRoute> = listOf(AppRoute.HOME)) {
    private var entries by mutableStateOf(initial)
    operator fun getValue(thisRef:Any?,property:KProperty<*>):String=entries.last().name.lowercase()
    operator fun setValue(thisRef:Any?,property:KProperty<*>,value:String) {
        val target=AppRoute.valueOf(value.uppercase())
        if(target in listOf(AppRoute.HOME,AppRoute.PROGRAMS,AppRoute.PROGRESS,AppRoute.SETTINGS))entries=listOf(target)
        else if(entries.last()!=target)entries=entries+target
    }
    fun back() {entries=if(entries.size>1)entries.dropLast(1) else listOf(AppRoute.HOME)}
    companion object {val Saver=listSaver<AppNavigator,String>(save={it.entries.map {r->r.name}},restore={AppNavigator(it.map {name->AppRoute.valueOf(name)})})}
}
internal val pt:java.util.Locale get()=java.util.Locale.forLanguageTag("pt-BR")
internal val dayNames=listOf("SEG","TER","QUA","QUI","SEX","SÁB","DOM")
internal fun duration(ms:Long):String {
    val seconds=ms/1000
    return if(seconds<60)"$seconds s" else if(seconds%60==0L)"${seconds/60} min" else "${seconds/60} min ${seconds%60} s"
}
