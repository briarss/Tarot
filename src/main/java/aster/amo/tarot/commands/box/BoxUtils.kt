package aster.amo.tarot.commands.box

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.storage.pc.PCPosition
import com.cobblemon.mod.common.api.types.ElementalTypes
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.swap
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.server.level.ServerPlayer
import java.util.concurrent.CompletableFuture

object BoxUtils {
    fun releaseBox(player: ServerPlayer, box: Int) {
        val pc = Cobblemon.storage.getPC(player) ?: return
        pc.boxes[box].let { boxInstance ->
            val pokemonToRemove: MutableList<Pokemon> = mutableListOf()
            boxInstance.forEach { pokemon ->
                pokemonToRemove.add(pokemon)
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

    fun sortBox(player: ServerPlayer, box: Int, sortType: SortType) {
        val pc = Cobblemon.storage.getPC(player) ?: return
        val pokemon = pc.boxes[box].toMutableList().sortedWith(sortType.comparator)
        releaseBox(player, box)
        for (i in pokemon.indices) {
            pc.set(PCPosition(box, i), pokemon[i])
        }
        pc.sendTo(player)
    }

    fun suggestSortTypes(
        commandContext: CommandContext<CommandSourceStack>?,
        suggestionsBuilder: SuggestionsBuilder?
    ): CompletableFuture<Suggestions>? {
        return SharedSuggestionProvider.suggest(
            SortType.values().map { it.name.toLowerCase() },
            suggestionsBuilder
        )
    }


    enum class SortType(val comparator: Comparator<Pokemon>) {
        NATIONAL_DEX_NUMBER(Comparator.comparingInt { it.species.nationalPokedexNumber }),
        NAME(Comparator.comparing { it.species.name }),
        LEVEL(Comparator.comparingInt { it.level }),
        TYPE(Comparator.comparing { ElementalTypes.all().indexOf(it.species.primaryType) }),
    }
}