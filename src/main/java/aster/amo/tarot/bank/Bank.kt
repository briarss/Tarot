package aster.amo.tarot.bank

import com.cobblemon.mod.common.pokemon.Pokemon
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.server.level.ServerPlayer
import java.util.UUID

class Bank(
    val player: UUID,
    val pokemon: MutableList<Pokemon>
) {
    companion object {
        val CODEC: Codec<Bank> = RecordCodecBuilder.create {
            it.group(
                Codec.STRING.fieldOf("player").forGetter { it.player.toString() },
                Codec.list(Pokemon.CODEC).fieldOf("pokemon").forGetter { it.pokemon }
            ).apply(it) { player, pokemon ->
                Bank(UUID.fromString(player), pokemon.toMutableList())
            }
        }
    }
}