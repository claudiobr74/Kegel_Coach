package com.pausa.domain

/** Educational preview only; never starts a workout or records history. */
enum class DemoPhase { PREPARE, CONTRACT, HOLD, RELAX }
data class DemoFrame(val phase: DemoPhase, val asset: String, val seconds: Int)

fun manualDemoFrame(quick: Boolean, elapsedMillis: Long): DemoFrame {
    val time = elapsedMillis.coerceAtLeast(0L) % if (quick) 2_000L else 10_000L
    if (quick) return if (time < 1_000L)
        DemoFrame(DemoPhase.CONTRACT, "05_rapida_contraia.png", 1)
    else DemoFrame(DemoPhase.RELAX, "06_rapida_relaxe.png", 1)
    return when {
        time < 1_000L -> DemoFrame(DemoPhase.PREPARE, "01_prepare.png", 1)
        time < 2_000L -> DemoFrame(DemoPhase.CONTRACT, "02_contraia.png", ((4_000L-time+999)/1_000).toInt())
        time < 4_000L -> DemoFrame(DemoPhase.HOLD, "03_mantenha.png", ((4_000L-time+999)/1_000).toInt())
        else -> DemoFrame(DemoPhase.RELAX, "04_relaxe.png", ((10_000L-time+999)/1_000).toInt())
    }
}
