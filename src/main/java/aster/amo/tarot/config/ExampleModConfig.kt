package aster.amo.tarot.config

import aster.amo.tarot.shiny.ShinyModifier
import com.cobblemon.mod.common.api.molang.ExpressionLike
import com.cobblemon.mod.common.util.asExpressionLike

class TarotConfig(
    var debug: Boolean = false,
    var connectionString: String = "mongodb://localhost:27017",
    var bankEnabled: Boolean = true,
    var shinyBoosts: MutableList<ShinyModifier> = mutableListOf(),
    var playerLevelSpawnEquation: ExpressionLike? = null,
) {
    override fun toString(): String {
        return "ExampleModConfig(debug=$debug)"
    }
}
