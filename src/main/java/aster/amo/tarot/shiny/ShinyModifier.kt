package aster.amo.tarot.shiny

import java.util.UUID

data class ShinyModifier(
    val startTime: Long = 0,
    val endTime: Long = 0,
    val modifier: Double = 0.0,
    val uuid: UUID = UUID.randomUUID()
)