package aster.amo.tarot.commands.subcommands

import aster.amo.ceremony.utils.TimeParser
import aster.amo.ceremony.utils.extension.get
import aster.amo.ceremony.utils.parseToNative
import aster.amo.tarot.Tarot
import aster.amo.tarot.config.ConfigManager
import aster.amo.tarot.data.TarotDataObject
import aster.amo.tarot.shiny.ShinyModifier
import aster.amo.tarot.utils.SubCommand
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument

class ShinyCommand : SubCommand {
    override fun build(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("shinyboost")
            .requires(Permissions.require("${Tarot.MOD_ID}.command.shinyboost", 2))
            .then(
                Commands.argument("player", EntityArgument.players())
                    .then(
                        Commands.argument("boost", DoubleArgumentType.doubleArg())
                            .then(
                                Commands.argument("end time", StringArgumentType.string())
                                    .executes(Companion::execute)
                            )
                    )
            )
            .build()
    }

    companion object {
        fun execute(ctx: CommandContext<CommandSourceStack>): Int {
            val players = EntityArgument.getPlayers(ctx, "player")
            val boost = DoubleArgumentType.getDouble(ctx, "boost")
            val endTime = StringArgumentType.getString(ctx, "end time")

            val parsedTime = try { TimeParser.parseTime(endTime) } catch (e: IllegalArgumentException) {
                e.message?.parseToNative()?.let { ctx.source.sendFailure(it) }
                Tarot.LOGGER.error("Failed to parse time", e)
                return 0
            }

            val modifier = ShinyModifier(System.currentTimeMillis(), parsedTime, boost)
            players.forEach { player ->
                val data = player get TarotDataObject
                data.shinyModifiers.add(modifier)
                ctx.source.sendMessage("Successfully added shiny modifier to ${player.name.string}".parseToNative())
            }

            ConfigManager.CONFIG.shinyBoosts.add(modifier)
            ConfigManager.saveFile("config.json", ConfigManager.CONFIG)
            return 1
        }
    }
}
