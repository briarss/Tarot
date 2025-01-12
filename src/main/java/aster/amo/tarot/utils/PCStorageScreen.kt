package aster.amo.tarot.utils

import aster.amo.ceremony.utils.MenuUtils
import aster.amo.ceremony.utils.parseToNative
import com.cobblemon.mod.common.api.storage.pc.PCPosition
import com.cobblemon.mod.common.api.storage.pc.PCStore
import com.cobblemon.mod.common.item.PokemonItem
import com.cobblemon.mod.common.pokemon.Pokemon
import eu.pb4.sgui.api.elements.GuiElementBuilder
import eu.pb4.sgui.api.gui.SimpleGui
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.Items
import net.minecraft.world.item.ItemStack

class PCStorageScreen(
    private val player: ServerPlayer,
    private val pcStorage: PCStore, // Interface to access PC storage
    private val onPokemonClick: (Pokemon, PCPosition) -> Unit, // Callback for Pokémon clicks
    private val previousScreen: SimpleGui? = null // Optional previous GUI to return to
) : SimpleGui(MenuType.GENERIC_9x6, player, false) {

    private var currentPage = 0
    private val totalPages = 30
    private val slotsPerPage = 30

    // Multi-Select Mode Variables
    private var multiSelectMode: Boolean = false
    private val selectedPokemons: MutableSet<PCPosition> = mutableSetOf()

    init {
        title = "Pokémon PC Storage".parseToNative()
        populateNavigationButtons()
        populatePokemonSlots()
        refresh()
    }

    /**
     * Populates the navigation buttons: Close, Previous Page, Next Page, Multi-Select Toggle, and Confirm.
     */
    private fun populateNavigationButtons() {
        // Clear existing navigation buttons to avoid duplicates on refresh
        clearNavigationButtons()

        // Close Button at top-left corner (0,0)
        setSlot(
            MenuUtils.getSlot(0, 0),
            GuiElementBuilder.from(Items.BARRIER.defaultInstance)
                .setName("Close".parseToNative())
                .setCallback { ctx ->
                    previousScreen?.open() ?: close()
                }
                .build()
        )

        // Previous Page Button at top-right corner (0,8)
        setSlot(
            MenuUtils.getSlot(0, 8),
            GuiElementBuilder.from(Items.ARROW.defaultInstance)
                .setName("Previous Page".parseToNative())
                .setLore(listOf("Click to go to the previous page.".parseToNative()))
                .setCallback { ctx ->
                    if (currentPage > 0) {
                        currentPage--
                        refresh()
                    } else {
                        player.sendSystemMessage("You are on the first page.".parseToNative())
                    }
                }
                .build()
        )

        // Next Page Button at bottom-right corner (5,8)
        setSlot(
            MenuUtils.getSlot(5, 8),
            GuiElementBuilder.from(Items.ARROW.defaultInstance)
                .setName("Next Page".parseToNative())
                .setLore(listOf("Click to go to the next page.".parseToNative()))
                .setCallback { ctx ->
                    if (currentPage < totalPages - 1) {
                        currentPage++
                        refresh()
                    } else {
                        player.sendSystemMessage("You are on the last page.".parseToNative())
                    }
                }
                .build()
        )

        // Page Indicator at bottom-center (5,4)
        setSlot(
            MenuUtils.getSlot(5, 4),
            GuiElementBuilder.from(Items.PAPER.defaultInstance)
                .setName("Page ${currentPage + 1}/$totalPages".parseToNative())
                .build()
        )

        // Multi-Select Toggle Button at top-center (0,4)
        setSlot(
            MenuUtils.getSlot(2, 0),
            GuiElementBuilder.from(
                if (multiSelectMode) Items.LIME_STAINED_GLASS_PANE.defaultInstance
                else Items.GRAY_STAINED_GLASS_PANE.defaultInstance
            )
                .setName(if (multiSelectMode) "Exit Multi-Select Mode".parseToNative()
                else "Enter Multi-Select Mode".parseToNative())
                .setLore(
                    listOf(
                        if (multiSelectMode) "Click to disable multi-select mode.".parseToNative()
                        else "Click to enable multi-select mode.".parseToNative()
                    )
                )
                .setCallback { ctx ->
                    multiSelectMode = !multiSelectMode
                    if (!multiSelectMode) {
                        selectedPokemons.clear()
                    }
                    refresh()
                }
                .build()
        )

        // Confirm Button at bottom-center (5,6) - Only visible in multi-select mode and when selections exist
        if (multiSelectMode && selectedPokemons.isNotEmpty()) {
            setSlot(
                MenuUtils.getSlot(3, 0),
                GuiElementBuilder.from(Items.EMERALD.defaultInstance)
                    .setName("Confirm Selection".parseToNative())
                    .setLore(listOf("Click to confirm your selection.".parseToNative()))
                    .setCallback { ctx ->
                        // Execute the callback for each selected Pokémon
                        for (position in selectedPokemons) {
                            val pokemon = pcStorage[position]
                            if (pokemon != null) {
                                onPokemonClick(pokemon, position)
                            }
                        }
                        // Clear selections and exit multi-select mode
                        selectedPokemons.clear()
                        multiSelectMode = false
                        refresh()
                    }
                    .build()
            )
        }
    }

    /**
     * Clears the navigation buttons to prevent duplication.
     */
    private fun clearNavigationButtons() {
        // Define all navigation button slots to clear
        val navigationSlots = listOf(
            MenuUtils.getSlot(0, 0), // Close
            MenuUtils.getSlot(2, 0), // Multi-Select Toggle
            MenuUtils.getSlot(0, 8), // Previous Page
            MenuUtils.getSlot(5, 8), // Next Page
            MenuUtils.getSlot(5, 4), // Page Indicator
            MenuUtils.getSlot(3, 0)  // Confirm (conditionally)
        )
        for (slot in navigationSlots) {
            setSlot(slot, GuiElementBuilder.from(Items.AIR.defaultInstance).build())
        }
    }

    /**
     * Populates the Pokémon slots based on the current page and multi-select mode.
     */
    private fun populatePokemonSlots() {
        clearPokemonSlots()

        for (index in 0 until slotsPerPage) {
            val position = PCPosition(currentPage, index)
            val pokemon = pcStorage[position]
            val guiSlot = getGuiSlotForIndex(index)
            if (guiSlot != null) {
                if (pokemon != null) {
                    val isSelected = multiSelectMode && selectedPokemons.contains(position)
                    val pokemonItem = PokemonItem.from(pokemon)
                    val displayItem: ItemStack = if (isSelected) {
                        val selectedItem = Items.GREEN_STAINED_GLASS_PANE.defaultInstance
                        selectedItem.set(DataComponents.ITEM_NAME, Component.literal("[Selected] ${pokemon.getDisplayName().string}"))
                        selectedItem
                    } else {
                        pokemonItem
                    }

                    val elementBuilder = GuiElementBuilder.from(displayItem)
                        .setName(pokemon.getDisplayName())
                        .setLore(pokemon.getSummary())

                    if (multiSelectMode) {
                        elementBuilder.setCallback { ctx ->
                            if (selectedPokemons.contains(position)) {
                                selectedPokemons.remove(position)
                                player.sendSystemMessage("Deselected ${pokemon.getDisplayName().string}.".parseToNative())
                            } else {
                                selectedPokemons.add(position)
                                player.sendSystemMessage("Selected ${pokemon.getDisplayName().string}.".parseToNative())
                            }
                            refresh()
                        }
                    } else {
                        elementBuilder.setCallback { ctx ->
                            onPokemonClick(pokemon, position)
                            previousScreen?.open() ?: close()
                        }
                    }

                    setSlot(
                        guiSlot,
                        elementBuilder.build()
                    )
                } else {
                    setSlot(
                        guiSlot,
                        GuiElementBuilder.from(Items.BLACK_STAINED_GLASS_PANE.defaultInstance)
                            .setName("Empty Slot".parseToNative())
                            .build()
                    )
                }
            }
        }
    }

    /**
     * Calculates and returns the GUI slot for a given index (0 to 29) to arrange them nicely.
     * Here, we arrange 30 slots in 5 columns and 6 rows, centered within the 9x6 grid.
     */
    private fun getGuiSlotForIndex(index: Int): Int? {
        val rows = 6
        val columns = 5
        val startCol = 2 // To center 5 columns in a 9-wide grid

        val row = index / columns
        val col = index % columns + startCol

        if (row >= rows) return null
        return MenuUtils.getSlot(row, col)
    }

    /**
     * Clears the Pokémon slots by setting them to AIR.
     */
    private fun clearPokemonSlots() {
        for (index in 0 until slotsPerPage) {
            val guiSlot = getGuiSlotForIndex(index)
            if (guiSlot != null) {
                setSlot(guiSlot, GuiElementBuilder.from(Items.AIR.defaultInstance).build())
            }
        }
    }

    /**
     * Refreshes the GUI to reflect the current state.
     */
    fun refresh() {
        populateNavigationButtons()
        populatePokemonSlots()
    }

    /**
     * Extension function to get a summary of the Pokémon.
     */
    private fun Pokemon.getSummary(): List<Component> {
        val summary = mutableListOf<Component>()
        summary.add("Level: $level".parseToNative())
        summary.add("Nature: ${nature.name}".parseToNative())
        summary.add("Ability: ${ability.name}".parseToNative())
        summary.add("Held Item: ${this.heldItem().displayName.string ?: "None"}".parseToNative())
        summary.add("Moves: ${this.moveSet.getMoves().map { it.displayName.string }.joinToString(", ")}".parseToNative())
        return summary
    }
}
