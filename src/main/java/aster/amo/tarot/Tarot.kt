package aster.amo.tarot

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
import soul.software.tarot.commands.BaseCommand
import soul.software.tarot.commands.BoxCommand
import soul.software.tarot.config.ConfigManager
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

    lateinit var adventure: FabricServerAudiences
    var server: MinecraftServer? = null

    var gson: Gson = GsonBuilder().disableHtmlEscaping().create()

    var gsonPretty: Gson = gson.newBuilder().setPrettyPrinting().create()
    override fun onInitialize() {
        INSTANCE = this

        this.configDir = File(FabricLoader.getInstance().configDirectory, MOD_ID)
        ConfigManager.load()

        registerEvents()
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
        }
    }

    fun reload() {
        ConfigManager.load()
    }
}
