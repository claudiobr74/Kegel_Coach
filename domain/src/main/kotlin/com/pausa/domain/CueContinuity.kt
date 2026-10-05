package com.pausa.domain

/** Protect haptic training from crediting time whose cues could not be delivered. */
object CueContinuity {
    fun interrupted(lastTickMillis:Long,nowMillis:Long,previousPhase:Int,currentPhase:Int):Boolean =
        nowMillis-lastTickMillis>1500L || currentPhase>previousPhase+1
}
