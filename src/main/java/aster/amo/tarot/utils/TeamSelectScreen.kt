package aster.amo.tarot.utils

import aster.amo.ceremony.utils.parseToNative
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.item.PokemonItem
import com.cobblemon.mod.common.pokemon.Pokemon
import eu.pb4.sgui.api.ClickType
import eu.pb4.sgui.api.elements.GuiElementBuilder
import eu.pb4.sgui.api.gui.SimpleGui
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.joml.Vector4f

class TeamSelectScreen(
    player: ServerPlayer,
    val onPokemonClick: (ClickType, Int) -> Unit,
    val selectablePredicate: (Pokemon) -> Boolean = { true }
) : SimpleGui(MenuType.GENERIC_9x6, player, true) {
    private val appButtons: MutableMap<Int, List<AppButton>> = mutableMapOf()
    init {
        setTitle("<blue>Select a Pokémon".parseToNative())
        val buttons = mutableListOf<AppButton>()
        val centerSlots = listOf(10, 13, 16, 37, 40, 43)
        val party = Cobblemon.storage.getParty(player)
        party.forEachIndexed { index, pokemon ->
            placePokemon(centerSlots[index], pokemon, buttons, index)
        }
        appButtons[0] = buttons
        refresh()
    }

    fun refresh() {
        val buttons = appButtons[0]
        buttons?.let { buttonList ->
            buttonList.forEach { button ->
                button.getSlots().forEach { slot ->
                    setSlot(slot, button.button.build())
                }
            }
        }
    }

    fun placePokemon(centerSlot: Int, pokemon: Pokemon, buttons: MutableList<AppButton>, pokemonIndex: Int) {
        val slots = listOf(centerSlot - 10, centerSlot - 9, centerSlot - 8, centerSlot - 1, centerSlot, centerSlot + 1, centerSlot + 8, centerSlot + 9, centerSlot + 10)
        slots.forEachIndexed { index, slot ->
            if(slot != centerSlot) {
                val x = slot % 9
                val y = slot / 9
                buttons.add(AppButton(x, y, 1, 1, GuiElementBuilder.from(Items.STONE_BUTTON.defaultInstance)
                    .setName(pokemon.getDisplayName())
                    .setCustomModelData(2)
                    .setLore(PokemonItemUtil.simpleSummary(pokemon))
                    .setCallback { ctx ->
                        if(selectablePredicate(pokemon)) onPokemonClick(ctx, pokemonIndex)
                        else player.sendMessage("<red>This Pokémon cannot be selected".parseToNative())
                    }))
            } else {
                val x = slot % 9
                val y = slot / 9
                val tint = if(!selectablePredicate(pokemon)) Vector4f(0.25f, 0.25f, 0.25f, 1.0f) else Vector4f(1f, 1f, 1f, 1.0f)
                buttons.add(AppButton(x, y, 1, 1, GuiElementBuilder.from(PokemonItem.from(pokemon, 1, tint))
                    .setName(pokemon.getDisplayName())
                    .setCustomModelData(PokemonItemUtil.LARGE_MODEL)
                    .setLore(PokemonItemUtil.simpleSummary(pokemon))
                    .setCallback { ctx ->
                        if(selectablePredicate(pokemon)) onPokemonClick(ctx, pokemonIndex)
                        else player.sendMessage("<red>This Pokémon cannot be selected".parseToNative())
                    }))
            }
        }
    }
}