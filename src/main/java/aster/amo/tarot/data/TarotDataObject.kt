package aster.amo.tarot.data

import aster.amo.ceremony.data.DataObject
import aster.amo.ceremony.data.DataObjectKey
import aster.amo.ceremony.data.PlayerData
import aster.amo.ceremony.utils.extension.get
import aster.amo.tarot.bank.Bank
import aster.amo.tarot.shiny.ShinyModifier
import com.cobblemon.mod.common.api.events.CobblemonEvents
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NbtOps
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player

class TarotDataObject(player: Player) : DataObject(player) {
    var bank: Bank = Bank(player.uuid, mutableListOf())
    val shinyModifiers: MutableList<ShinyModifier> = mutableListOf()
    override fun readFromNbt(tag: CompoundTag) {
        if(tag.contains("bank")) {
            val result = Bank.CODEC.decode(NbtOps.INSTANCE, tag.get("bank"))
            if(result.error().isPresent) {
                throw IllegalStateException("Failed to decode bank data")
            }
            result.result().ifPresent { bank = it.first }
        }

        if(tag.contains("shinyModifiers")) {
            val listTag = tag.getList("shinyModifiers", 10)
            for(i in 0 until listTag.size) {
                val compoundTag = listTag.getCompound(i)
                val startTime = compoundTag.getLong("startTime")
                val endTime = compoundTag.getLong("endTime")
                val modifier = compoundTag.getDouble("modifier")
                shinyModifiers.add(ShinyModifier(startTime, endTime, modifier))
            }
        }
    }

    override fun writeToNbt(tag: CompoundTag) {
        bank?.let {
            val result = Bank.CODEC.encodeStart(NbtOps.INSTANCE, it)
            if(result.error().isPresent) {
                throw IllegalStateException("Failed to encode bank data")
            }
            tag.put("bank", result.result().get())
        }

        val listTag = ListTag()
        shinyModifiers.forEach {
            val tag = CompoundTag()
            tag.putLong("startTime", it.startTime)
            tag.putLong("endTime", it.endTime)
            tag.putDouble("modifier", it.modifier)
            listTag.add(tag)
        }
        tag.put("shinyModifiers", listTag)
    }

    companion object Key : DataObjectKey<TarotDataObject> {
        val RL = ResourceLocation.parse("tarot:tarot_data_object")
        init {
            PlayerData.setupData {
                PlayerData.registerData(RL, TarotDataObject::class.java)
            }

            CobblemonEvents.SHINY_CHANCE_CALCULATION.subscribe { event ->
                event.addModificationFunction { baseChance, serverPlayer, pokemon ->
                    if(serverPlayer == null) return@addModificationFunction baseChance
                    val data = serverPlayer get TarotDataObject
                    if(data != null) {
                        data.shinyModifiers.removeIf { it.endTime < System.currentTimeMillis() }
                        val modifiers = data.shinyModifiers.filter { it.startTime <= System.currentTimeMillis() && it.endTime >= System.currentTimeMillis() }
                        if(modifiers.isNotEmpty()) {
                            // base chance is 8192, so we need to invert the modifier
                            val modifier = modifiers.map { it.modifier }.reduce { acc, d -> acc * d }
                            return@addModificationFunction (baseChance / modifier).toFloat()
                        }
                    }
                    return@addModificationFunction baseChance
                }
            }
        }
    }
}