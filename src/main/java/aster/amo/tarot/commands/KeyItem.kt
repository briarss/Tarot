package aster.amo.tarot.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.tree.LiteralCommandNode
import aster.amo.tarot.Tarot
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import aster.amo.tarot.commands.subcommands.BoxReleaseCommand
import aster.amo.tarot.commands.subcommands.BoxResizeCommand
import aster.amo.tarot.commands.subcommands.BoxSwapCommand
import aster.amo.tarot.commands.subcommands.SortBoxCommand
import aster.amo.tarot.config.ConfigManager
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.util.asResource
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.arguments.StringArgumentType.getString
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.LivingEntity

class KeyItem {
    private val aliases = listOf("keyitem")

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        val rootCommands: List<LiteralCommandNode<CommandSourceStack>> = aliases.map {
            Commands.literal(it)
                .requires(Permissions.require("${Tarot.MOD_ID}.command.keyitem", 2))
                .then(Commands.argument("operation", StringArgumentType.string())
                    .then(Commands.argument("key item", StringArgumentType.greedyString())
                        .executes { context ->
                            val operation = getString(context, "operation")
                            val keyItem = getString(context, "key item")
                            val player = context.source.playerOrException
                            when(operation) {
                                "add" -> {
                                    Cobblemon.playerDataManager.getGenericData((player as ServerPlayer)).keyItems.add(keyItem.asResource())
                                }
                                "remove" -> {
                                    val keyItems = Cobblemon.playerDataManager.getGenericData(player).keyItems
                                    keyItems.removeIf { it == keyItem.asResource() }
                                }
                                else -> {
                                    // Invalid operation
                                }
                            }
                            1
                        }))
                .build()
        }

        rootCommands.forEach { root ->
            dispatcher.root.addChild(root)
        }
    }
}
