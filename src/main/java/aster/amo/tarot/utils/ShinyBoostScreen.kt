package aster.amo.tarot.utils

import aster.amo.ceremony.utils.MenuUtils
import aster.amo.ceremony.utils.TimeParser
import aster.amo.ceremony.utils.Utils
import aster.amo.ceremony.utils.extension.get
import aster.amo.ceremony.utils.parseToNative
import aster.amo.tarot.data.TarotDataObject
import eu.pb4.sgui.api.elements.GuiElementBuilder
import eu.pb4.sgui.api.gui.SimpleGui
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBundlePacket
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.Items
import java.util.ArrayList
import java.util.concurrent.TimeUnit

class ShinyBoostScreen(player: ServerPlayer) : SimpleGui(MenuType.GENERIC_9x6, player, false) {

    init {
        populateBoostItems()
        setupNavigationButtons()
        refresh()
    }

    private fun refresh() {
        clear()
        populateBoostItems()
        setupNavigationButtons()
        setTitle("Shiny Boosts".parseToNative())
    }

    override fun onTick() {
        super.onTick()
        refresh()
    }

    override fun setTitle(title: Component) {
        if (this.isOpen) {
            val list: ArrayList<Packet<ClientGamePacketListener>> = ArrayList<Packet<ClientGamePacketListener>>()
            list.add(ClientboundOpenScreenPacket(this.syncId, this.type, title))
            list.add(
                ClientboundContainerSetContentPacket(
                    this.syncId, screenHandler.stateId,
                    screenHandler.items, screenHandler.carried
                )
            )

            player.connection.send(ClientboundBundlePacket(list))
            for (i in screenHandler.slots.indices) {
                screenHandler.setRemoteSlot(
                    i,
                    screenHandler.slots[i].item.copy()
                )
            }
            screenHandler.carried = screenHandler.carried
        }
    }

    private fun clear() {
        for (index in 0 until 54) {
            val guiSlot = getGuiSlotForIndex(index)
            if (guiSlot != null) {
                setSlot(guiSlot, GuiElementBuilder.from(Items.AIR.defaultInstance).build())
            }
        }
    }
    private fun getGuiSlotForIndex(index: Int): Int? {
        val rows = 6
        val columns = 5
        val startCol = 2 // To center 5 columns in a 9-wide grid

        val row = index / columns
        val col = index % columns + startCol

        if (row >= rows) return null
        return MenuUtils.getSlot(row, col)
    }
    private fun populateBoostItems() {
        val data = player get TarotDataObject
        val boosts = data.shinyModifiers.filter { it.endTime > System.currentTimeMillis() }
        val sortedBoosts = boosts.sortedBy { it.endTime }

        if(sortedBoosts.isEmpty()) {
            val itemStack = Items.PAPER.defaultInstance
            val guiElement = GuiElementBuilder.from(itemStack)
                .setName("<gold>No active shiny boosts".parseToNative())
            setSlot(MenuUtils.getSlot(1, 1), guiElement.build())
            return
        }
        for ((index, boost) in sortedBoosts.withIndex()) {
            val boostPercent = boost.modifier * 100
            val time = TimeParser.formatCountdown(boost.endTime)

            val tooltipLines = listOf(
                "<blue>Time Remaining: $time".parseToNative()
            )

            val itemStack = Items.PAPER.defaultInstance
            val guiElement = GuiElementBuilder.from(itemStack)
                .setName("<gold>Shiny Boost: ${"%.0f".format(boostPercent)}x".parseToNative())
                .setLore(tooltipLines)
            val row = 1 + (index / 7)
            val col = 1 + (index % 7)
            setSlot(MenuUtils.getSlot(row, col), guiElement.build())
        }
    }

    private fun setupNavigationButtons() {
        setSlot(
            MenuUtils.getSlot(0, 0),
            GuiElementBuilder.from(Items.BARRIER.defaultInstance)
                .setName("Close".parseToNative())
                .setCallback { ctx -> close() }
                .build()
        )
    }
}
