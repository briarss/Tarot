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
import aster.amo.tarot.utils.ShinyBoostScreen
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.util.asResource
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.arguments.StringArgumentType.getString
import net.minecraft.server.level.ServerPlayer

class ShinyBoost {
    private val aliases = listOf("shinyboost")

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        val rootCommands: List<LiteralCommandNode<CommandSourceStack>> = aliases.map {
            Commands.literal(it)
                .requires(Permissions.require("${Tarot.MOD_ID}.command.shinyboosts", 2))
                .executes { ctx ->
                    val source = ctx.source
                    val player = source.playerOrException
                    ShinyBoostScreen(player).open()
                    1
                }
                .build()
        }

        rootCommands.forEach { root ->
            dispatcher.root.addChild(root)
        }
    }
}
