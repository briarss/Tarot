package aster.amo.tarot.utils

import aster.amo.ceremony.utils.extension.setLore
import aster.amo.ceremony.utils.parseToNative
import com.cobblemon.mod.common.item.PokemonItem
import com.cobblemon.mod.common.pokemon.Pokemon
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.item.ItemStack

object PokemonItemUtil {
    val BASE_MODEL: Int = 0
    val MEDIUM_MODEL: Int = 1
    val LARGE_MODEL: Int = 2


    fun simpleSummary(pokemon: Pokemon): List<MutableComponent> {
        val list = mutableListOf<MutableComponent>()
        val types = "<gold> ▶ Type<white>: ".parseToNative().copy()
        pokemon.types.forEach { type ->
            types.append(type.displayName)
            if(pokemon.types.indexOf(type) < pokemon.types.spliterator().exactSizeIfKnown - 1) types.append(Component.literal(" / "))
        }
        list.add(types)
        list.add("<gold> ▶ Level<white>: ${pokemon.level}".parseToNative().copy())
        return list
    }

    fun init(){

    }
}

fun Pokemon.asItemStack(): ItemStack {
    val pokemon = this
    return PokemonItem.from(this).apply {
        this.setLore(PokemonItemUtil.simpleSummary(pokemon))
    }
}