package aster.amo.tarot.commands.subcommands

import aster.amo.ceremony.utils.parseToNative
import aster.amo.tarot.Tarot
import aster.amo.tarot.commands.box.BoxUtils
import aster.amo.tarot.utils.SubCommand
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import me.lucko.fabric.api.permissions.v0.Permissions
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument

class BoxResizeCommand : SubCommand {
    override fun build(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("resize")
            .requires(Permissions.require("${Tarot.MOD_ID}.command.box.resize", 2))
            .then(
                Commands.argument("player", EntityArgument.players())
                    .then(
                        Commands.argument(
                            "size", IntegerArgumentType.integer()
                        )
                            .executes { ctx: CommandContext<CommandSourceStack> ->
                                val size = IntegerArgumentType.getInteger(ctx, "size")
                                val players = EntityArgument.getPlayers(ctx, "player")
                                players.forEach { player ->
                                    BoxUtils.resizeMaxBoxCount(player, size)
                                }
                                1
                            }
                    )
            )
            .build()
    }
}
