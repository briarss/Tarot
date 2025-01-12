package aster.amo.tarot.bank

import aster.amo.ceremony.utils.MenuUtils
import aster.amo.ceremony.utils.parseToNative
import aster.amo.tarot.Tarot
import aster.amo.tarot.utils.PCStorageScreen
import aster.amo.tarot.utils.TeamSelectScreen
import aster.amo.tarot.utils.inform
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore
import com.cobblemon.mod.common.item.PokemonItem
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.party
import com.cobblemon.mod.common.util.pc
import com.cobblemon.mod.common.util.toVec3d
import eu.pb4.sgui.api.elements.GuiElementBuilder
import eu.pb4.sgui.api.gui.SimpleGui
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.Items

class BankScreen(
    private val player: ServerPlayer,
    private val bank: Bank,
    private val previousScreen: SimpleGui? // The screen to return to
) : SimpleGui(MenuType.GENERIC_9x6, player, false) {

    private var currentPage = 0
    private val itemsPerPage = 45 // 5 rows * 9 columns

    init {
        title = "Pokémon Bank".parseToNative()
        populateManagementButtons()
        populatePokemonSlots()
        setupNavigationButtons()
        refresh()
    }

    /**
     * Populates the top row with management buttons.
     */
    private fun populateManagementButtons() {
        // Close Button - y=0, x=0
        setSlot(
            MenuUtils.getSlot(0, 0),
            GuiElementBuilder.from(Items.BARRIER.defaultInstance)
                .setName("Close".parseToNative())
                .setCallback { ctx ->
                    previousScreen?.open() ?: close()
                }
                .build()
        )

        // Previous Page Button - y=0, x=1
        setSlot(
            MenuUtils.getSlot(0, 1),
            GuiElementBuilder.from(Items.ARROW.defaultInstance)
                .setName("Previous Page".parseToNative())
                .setLore(listOf("Click to go to the previous page.".parseToNative()))
                .setCallback { ctx ->
                    if (currentPage > 0) {
                        currentPage--
                        refresh()
                    }
                }
                .build()
        )

        // Add Pokémon Button - y=0, x=4
        setSlot(
            MenuUtils.getSlot(0, 4),
            GuiElementBuilder.from(Items.APPLE.defaultInstance) // Replace with a suitable item
                .setName("Add Pokémon".parseToNative())
                .setLore(listOf("Click to add a Pokémon to the pasture.".parseToNative()))
                .setCallback { ctx ->
                    val thisGui = this
                    if(ctx.isLeft) {
                        TeamSelectScreen(
                            player,
                            { ctx, index ->
                                val pokemon = player.party().get(index) ?: return@TeamSelectScreen
                                if (bank.pokemon.size < 100) {
                                    bank.pokemon.add(pokemon)
                                    player.party().remove(pokemon)
                                    // the party is a 0-5 indexed list, and entries can be null, so we need to squash the list to remove nulls and reindex
                                    player.party().squash()
                                    thisGui.open()
                                    thisGui.refresh()
                                } else {
                                    player.inform("The pasture is full.".parseToNative())
                                }
                            },
                            {
                                bank.pokemon.size < 100
                            }
                        ).open()
                    } else if (ctx.isRight) {
                        PCStorageScreen(
                            player,
                            Cobblemon.storage.getPC(player),
                            onPokemonClick = { ctx, index ->
                                val pokemon = player.pc()[index] ?: return@PCStorageScreen
                                if (bank.pokemon.size < 100) {
                                    bank.pokemon.add(pokemon)
                                    player.pc().remove(pokemon)
                                } else {
                                    player.inform("The pasture is full.".parseToNative())
                                }
                            },
                            previousScreen =  this
                        ).open()
                    }

                }
                .build()
        )

        // Next Page Button - y=0, x=7
        setSlot(
            MenuUtils.getSlot(0, 7),
            GuiElementBuilder.from(Items.ARROW.defaultInstance)
                .setName("Next Page".parseToNative())
                .setLore(listOf("Click to go to the next page.".parseToNative()))
                .setCallback { ctx ->
                    val totalPages = calculateTotalPages()
                    if (currentPage < totalPages - 1) {
                        currentPage++
                        refresh()
                    }
                }
                .build()
        )
    }

    /**
     * Populates the Pokémon slots with Pokémon from the pasture based on the current page.
     */
    private fun populatePokemonSlots() {
        clearPokemonSlots()
        val startIndex = currentPage * itemsPerPage
        val endIndex = minOf(startIndex + itemsPerPage, bank.pokemon.size)
        val pokemonToDisplay = bank.pokemon.stream().toList().subList(startIndex, endIndex)

        for (i in 0 until pokemonToDisplay.size) {
            val row = 1 + (i / 9)
            val col = i % 9
            val item = PokemonItem.Companion.from(pokemonToDisplay[i])
            setSlot(
                MenuUtils.getSlot(row, col),
                GuiElementBuilder.from(item)
                    .setCallback { ctx ->
                        if (player.party().add(pokemonToDisplay[i])) {
                            bank.pokemon.remove(pokemonToDisplay[i])
                            player.inform("The Pokémon was added to your party.".parseToNative())
                        } else {
                            if (player.pc().add(pokemonToDisplay[i])) {
                                bank.pokemon.remove(pokemonToDisplay[i])
                                player.inform("The Pokémon was added to your PC.".parseToNative())
                            } else {
                                player.inform("You have no room in your party or PC.".parseToNative())
                            }
                        }
                        clearPokemonSlots()
                        refresh()
                    }
                    .build()
            )
        }
    }

    private fun clearPokemonSlots() {
        for (i in 0 until itemsPerPage) {
            val row = 1 + (i / 9)
            val col = i % 9
            setSlot(MenuUtils.getSlot(row, col), GuiElementBuilder.from(Items.AIR.defaultInstance).build())
        }
    }

    /**
     * Sets up additional navigation buttons if needed.
     */
    private fun setupNavigationButtons() {
        // Currently, all management buttons are in the top row.
        // Additional buttons can be added here if required.
    }

    /**
     * Calculates the total number of pages based on the number of Pokémon.
     */
    private fun calculateTotalPages(): Int {
        return if (bank.pokemon.isEmpty()) {
            1
        } else {
            (bank.pokemon.size + itemsPerPage - 1) / itemsPerPage
        }
    }

    /**
     * Refreshes the GUI to reflect the current state.
     */
    fun refresh() {
        populateManagementButtons()
        populatePokemonSlots()
        setupNavigationButtons()
    }
}

private fun PlayerPartyStore.squash() {
    val newParty = mutableListOf<Pokemon?>()
    for (i in 0..5) {
        val pokemon = get(i)
        if (pokemon != null) {
            newParty.add(pokemon)
        }
    }
    for (i in newParty.size until 5) {
        newParty.add(null)
    }
    clearParty()
    newParty.forEachIndexed { index, pokemon ->
        if (pokemon != null) {
            set(index, pokemon)
        }
    }
}
