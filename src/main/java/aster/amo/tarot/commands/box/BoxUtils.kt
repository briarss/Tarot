package aster.amo.tarot.commands.box

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.swap
import net.minecraft.server.level.ServerPlayer

object BoxUtils {
    fun releaseBox(player: ServerPlayer, box: Int) {
        val pc = Cobblemon.storage.getPC(player) ?: return
        pc.boxes[box].let { boxInstance ->
            val pokemonToRemove: MutableList<Pokemon> = mutableListOf()
            for (position in 0..boxInstance.getNonEmptySlots().size) {
                val pokemon = boxInstance.getNonEmptySlots()[position]
                if (pokemon != null) {
                    pokemonToRemove.add(pokemon)
                }
            }
            for (pokemon in pokemonToRemove) {
                boxInstance.pc.remove(pokemon)
            }
        }
    }

    fun swapBoxes(player: ServerPlayer, box1: Int, box2: Int) {
        val pc = Cobblemon.storage.getPC(player) ?: return
        pc.boxes.swap(box1, box2)
        pc.sendTo(player)

    }
}