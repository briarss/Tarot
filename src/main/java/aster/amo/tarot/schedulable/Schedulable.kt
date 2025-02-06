package aster.amo.tarot.schedulable

import aster.amo.ceremony.utils.TimeParser
import com.bedrockk.molang.runtime.MoLangRuntime
import com.cobblemon.mod.common.api.molang.MoLangFunctions.addFunctions
import com.cobblemon.mod.common.api.molang.MoLangFunctions.asMoLangValue
import com.cobblemon.mod.common.api.molang.MoLangFunctions.setup
import com.cobblemon.mod.common.util.asExpressionLike
import com.cobblemon.mod.common.util.resolveBoolean
import net.minecraft.server.level.ServerLevel

class Schedulable(
    val command: String = "",
    val time: String = "",
    val filter: String = "",
    var repeat: Boolean = true
) {
    var reset: Long = 0L
    var hasFired = false

    init {
        if(time.isNotEmpty() && !repeat) {
            reset = TimeParser.parseTime(time)
        }
    }
    fun tick(level: ServerLevel) {
        if (System.currentTimeMillis() >= reset && (!hasFired || repeat)) {
            reset = TimeParser.parseTime(time)
            hasFired = true

            level.server.playerList.players.forEach { player ->
                val runtime = MoLangRuntime().setup()
                runtime.environment.query.addFunctions(
                    mapOf(
                        "player" to java.util.function.Function { params ->
                            return@Function player.asMoLangValue()
                        }
                    )
                )
                if (filter.isEmpty() || runtime.resolveBoolean(filter.asExpressionLike())) {
                    player.level().server!!.commands.dispatcher.execute(
                        command.replace(
                            "{player}",
                            player.gameProfile.name
                        ), player.createCommandSourceStack()
                    )
                }
            }
        }
    }

    companion object Schedulables {
        val schedulables = mutableListOf<Schedulable>()

        fun tick(level: ServerLevel) {
            schedulables.forEach { it.tick(level) }
        }
    }
}