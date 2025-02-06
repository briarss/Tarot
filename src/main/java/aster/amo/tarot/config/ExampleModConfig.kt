package aster.amo.tarot.config

import aster.amo.tarot.shiny.ShinyModifier

class TarotConfig(
    var debug: Boolean = false,
    var connectionString: String = "mongodb://localhost:27017",
    var bankEnabled: Boolean = true,
    var shinyBoosts: MutableList<ShinyModifier> = mutableListOf()
) {
    override fun toString(): String {
        return "ExampleModConfig(debug=$debug)"
    }
}
