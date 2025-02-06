package aster.amo.tarot.commands

import aster.amo.ceremony.utils.extension.get
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.tree.LiteralCommandNode
import aster.amo.tarot.Tarot
import aster.amo.tarot.bank.Bank
import aster.amo.tarot.bank.BankScreen
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import aster.amo.tarot.commands.subcommands.BoxReleaseCommand
import aster.amo.tarot.commands.subcommands.BoxSwapCommand
import aster.amo.tarot.commands.subcommands.SortBoxCommand
import aster.amo.tarot.config.ConfigManager
import aster.amo.tarot.data.TarotDataObject
import kotlinx.coroutines.runBlocking

class BankCommand {
    private val aliases = listOf("bank")

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        if(!ConfigManager.CONFIG.bankEnabled) return
        val rootCommands: List<LiteralCommandNode<CommandSourceStack>> = aliases.map {
            Commands.literal(it)
                .requires(Permissions.require("${Tarot.MOD_ID}.command.box", 2))
                .executes { ctx ->
                    val player = ctx.source.playerOrException
                    val bank = (player get TarotDataObject).bank ?: return@executes 0
                    BankScreen(player, bank, null).open()
                    1
                }
                .build()
        }

        val subCommands: List<LiteralCommandNode<CommandSourceStack>> = listOf(
        )

        rootCommands.forEach { root ->
            subCommands.forEach { sub -> root.addChild(sub) }
            dispatcher.root.addChild(root)
        }
    }
}
