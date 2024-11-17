package soul.software.tarot.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.tree.LiteralCommandNode
import aster.amo.tarot.Tarot
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import soul.software.tarot.commands.subcommands.BoxReleaseCommand
import soul.software.tarot.commands.subcommands.BoxSwapCommand
import soul.software.tarot.commands.subcommands.SortBoxCommand

class BoxCommand {
    private val aliases = listOf("box")

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        val rootCommands: List<LiteralCommandNode<CommandSourceStack>> = aliases.map {
            Commands.literal(it)
                .requires(Permissions.require("${Tarot.MOD_ID}.command.box", 2))
                .build()
        }

        val subCommands: List<LiteralCommandNode<CommandSourceStack>> = listOf(
            BoxReleaseCommand().build(),
            BoxSwapCommand().build(),
            SortBoxCommand().build()
        )

        rootCommands.forEach { root ->
            subCommands.forEach { sub -> root.addChild(sub) }
            dispatcher.root.addChild(root)
        }
    }
}
