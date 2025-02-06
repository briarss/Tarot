package aster.amo.tarot.commands

import aster.amo.tarot.utils.ShinyBoostScreen
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType.getString
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands

fun registerShinyBoostCommand(dispatcher: CommandDispatcher<CommandSourceStack>) {
    dispatcher.register(
        LiteralArgumentBuilder.literal<CommandSourceStack>("shinyboosts")
            .executes { ctx ->
                val source = ctx.source
                val player = source.playerOrException
                ShinyBoostScreen(player).open()
                1
            }
    )
}
