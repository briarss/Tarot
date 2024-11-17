package aster.amo.tarot.commands.subcommands

import aster.amo.tarot.commands.box.BoxUtils
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import aster.amo.tarot.Tarot
import com.mojang.brigadier.arguments.StringArgumentType
import aster.amo.tarot.utils.SubCommand
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands

class SortBoxCommand : SubCommand {
    override fun build(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("sort")
            .requires(Permissions.require("${Tarot.MOD_ID}.command.box.sort", 2))
            .then(Commands.argument("box", IntegerArgumentType.integer())
                .then(Commands.argument("sort type", StringArgumentType.string())
                    .suggests(BoxUtils::suggestSortTypes)
                    .executes { context: CommandContext<CommandSourceStack> ->
                        val box = IntegerArgumentType.getInteger(context, "box")-1
                        val sortType = StringArgumentType.getString(context, "sort type")
                        val player = context.source.playerOrException
                        BoxUtils.sortBox(player, box, BoxUtils.SortType.valueOf(sortType.uppercase()))
                        1
                    }
                )
            )
            .build()
    }
}
