package aster.amo.tarot

import aster.amo.tarot.bank.Bank
import aster.amo.tarot.bank.BankRepository
import aster.amo.tarot.commands.BankCommand
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.api.ModInitializer
import net.kyori.adventure.platform.fabric.FabricServerAudiences
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import aster.amo.tarot.commands.BaseCommand
import aster.amo.tarot.commands.BoxCommand
import aster.amo.tarot.config.ConfigManager
import aster.amo.tarot.utils.MongoUtils
import com.cobblemon.mod.common.util.math.geometry.toRadians
import kotlinx.coroutines.runBlocking
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket
import java.io.File

class Tarot : ModInitializer {
    companion object {
        lateinit var INSTANCE: Tarot

        var MOD_ID = "tarot"
        var MOD_NAME = "Tarot"

        val LOGGER: Logger = LogManager.getLogger(MOD_ID)
        val MINI_MESSAGE: MiniMessage = MiniMessage.miniMessage()

        @JvmStatic
        fun asResource(path: String): ResourceLocation {
            return ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
        }
    }

    lateinit var configDir: File
    @Volatile lateinit var repository: BankRepository

    lateinit var adventure: FabricServerAudiences
    var server: MinecraftServer? = null

    var gson: Gson = GsonBuilder().disableHtmlEscaping().create()

    var gsonPretty: Gson = gson.newBuilder().setPrettyPrinting().create()
    override fun onInitialize() {
        INSTANCE = this

        this.configDir = File(FabricLoader.getInstance().configDirectory, MOD_ID)
        ConfigManager.load()

        registerEvents()

        val client = MongoUtils.createMongoClient(gson)
        val repository = BankRepository(client)
        runBlocking {
            repository.init()
        }
        this.repository = repository
    }

    private fun registerEvents() {
        ServerLifecycleEvents.SERVER_STARTING.register(ServerLifecycleEvents.ServerStarting { server: MinecraftServer? ->
            this.adventure = FabricServerAudiences.of(
                server!!
            )
            this.server = server
        })
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            BaseCommand().register(
                dispatcher
            )
            BoxCommand().register(
                dispatcher
            )
            BankCommand().register(
                dispatcher
            )
        }

        ServerPlayConnectionEvents.JOIN.register { handler, sender, server ->
            val player = handler.player
            runBlocking {
                INSTANCE.repository.readPlayerData(player.uuid)?.let {} ?: INSTANCE.repository.writePlayerData(
                    player.uuid,
                    Bank(player.uuid, mutableListOf())
                )
            }
        }
    }

    fun reload() {
        ConfigManager.load()
    }
}
