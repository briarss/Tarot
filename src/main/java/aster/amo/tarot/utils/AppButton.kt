package aster.amo.tarot.utils

import aster.amo.ceremony.utils.MenuUtils
import eu.pb4.sgui.api.elements.GuiElementBuilder

class AppButton(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val button: GuiElementBuilder
) {
    fun getBaseSlot(): Int {
        return MenuUtils.getSlot(y, x)
    }

    fun getSlots(): List<Int> {
        val slots = mutableListOf<Int>()
        for (h in 0 until height) {
            for (w in 0 until width) {
                slots.add(getBaseSlot() + w + h * 9)
            }
        }
        return slots
    }
}
