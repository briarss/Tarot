package aster.amo.tarot.commands.subcommands

import aster.amo.ceremony.utils.parseToNative
import aster.amo.tarot.commands.box.BoxUtils
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import aster.amo.tarot.Tarot
import aster.amo.tarot.utils.SubCommand
import me.lucko.fabric.api.permissions.v0.Permissions
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands

class BoxReleaseCommand : SubCommand {
    override fun build(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("release")
            .requires(Permissions.require("${Tarot.MOD_ID}.command.box.release", 2))
            .then(Commands.argument("box", IntegerArgumentType.integer())
                .executes { ctx: CommandContext<CommandSourceStack> ->
                    val box = IntegerArgumentType.getInteger(ctx, "box")
                    val player = ctx.source.playerOrException
                    player.sendMessage(Component.text(" ℹ ").color(TextColor.fromHexString("#ff0000")).append(Component.text("Are you sure? Click here to confirm").color(TextColor.fromHexString("#ff9900")).clickEvent(ClickEvent.callback(ClickCallback { _ ->
                        BoxUtils.releaseBox(player, box-1)
                        player.sendSystemMessage("<green>Box $box released!".parseToNative())
                    })).decorate(TextDecoration.UNDERLINED)))
                    1
                }
            )
            .build()
    }
}
