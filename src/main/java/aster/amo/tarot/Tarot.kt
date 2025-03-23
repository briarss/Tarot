package aster.amo.tarot

import aster.amo.ceremony.utils.extension.get
import aster.amo.tarot.bank.Bank
import aster.amo.tarot.bank.BankRepository
import aster.amo.tarot.commands.*
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
import aster.amo.tarot.config.ConfigManager
import aster.amo.tarot.data.TarotDataObject
import aster.amo.tarot.schedulable.Schedulable
import aster.amo.tarot.shiny.ShinyModifier
import aster.amo.tarot.utils.MongoUtils
import com.bedrockk.molang.runtime.MoLangRuntime
import com.bedrockk.molang.runtime.struct.QueryStruct
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.molang.ExpressionLike
import com.cobblemon.mod.common.api.molang.MoLangFunctions.addEntityFunctions
import com.cobblemon.mod.common.api.molang.MoLangFunctions.addFunctions
import com.cobblemon.mod.common.api.molang.MoLangFunctions.addPokemonFunctions
import com.cobblemon.mod.common.api.molang.MoLangFunctions.asMoLangValue
import com.cobblemon.mod.common.api.molang.MoLangFunctions.setup
import com.cobblemon.mod.common.api.pokemon.evolution.PreEvolution
import com.cobblemon.mod.common.api.spawning.spawner.PlayerSpawner
import com.cobblemon.mod.common.pokemon.evolution.variants.LevelUpEvolution
import com.cobblemon.mod.common.util.adapters.ExpressionLikeAdapter
import com.cobblemon.mod.common.util.ifIsType
import com.cobblemon.mod.common.util.math.geometry.toRadians
import com.cobblemon.mod.common.util.party
import com.cobblemon.mod.common.util.resolveDouble
import com.cobblemon.mod.common.util.subscribeOnServer
import kotlinx.coroutines.runBlocking
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket
import java.io.File
import java.util.function.Function

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

    var gson: Gson = GsonBuilder()
        .registerTypeAdapter(ExpressionLike::class.java, ExpressionLikeAdapter)
        .disableHtmlEscaping()
        .create()

    var gsonPretty: Gson = gson.newBuilder().setPrettyPrinting().create()
    override fun onInitialize() {
        INSTANCE = this

        this.configDir = File(FabricLoader.getInstance().configDirectory, MOD_ID)
        ConfigManager.load()

        registerEvents()

        if(ConfigManager.CONFIG.bankEnabled) {
            val client = MongoUtils.createMongoClient(gson)
            val repository = BankRepository(client)
            runBlocking {
                repository.init()
            }
            this.repository = repository
        }
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
            ShinyBoost().register(
                dispatcher
            )
            KeyItem().register(
                dispatcher
            )
        }

        ServerPlayConnectionEvents.JOIN.register { handler, sender, server ->
            if(ConfigManager.CONFIG.bankEnabled) {
                val player = handler.player
                runBlocking {
                    INSTANCE.repository.readPlayerData(player.uuid)?.let {} ?: INSTANCE.repository.writePlayerData(
                        player.uuid,
                        Bank(player.uuid, mutableListOf())
                    )
                }
            }
            ConfigManager.CONFIG.shinyBoosts.forEach { boost ->
                val playerData = handler.player get TarotDataObject
                if(playerData.shinyModifiers.none { it.uuid == boost.uuid }) {
                    playerData.shinyModifiers.add(boost)
                }
            }
        }

        ServerTickEvents.START_SERVER_TICK.register { server ->
            if(server.overworld() != null) Schedulable.tick(server.overworld())
            val toRemove = mutableListOf<ShinyModifier>()
            ConfigManager.CONFIG.shinyBoosts.forEach { boost ->
                if(boost.endTime < System.currentTimeMillis()) {
                    toRemove.add(boost)
                }
            }
            ConfigManager.CONFIG.shinyBoosts.removeAll(toRemove)
            if(toRemove.isNotEmpty()) ConfigManager.saveFile("config.json", ConfigManager.CONFIG)
        }

        CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe { event ->
            event.ctx.spawner.ifIsType<PlayerSpawner> {
                val player = event.entity.level().server!!.playerList.getPlayer(this.uuid) ?: return@ifIsType
                val runtime = MoLangRuntime().setup()
                runtime.environment.query.addFunctions(mapOf(
                    "player" to Function { params -> player.asMoLangValue()},
                    "pokemon" to Function { params -> event.entity.pokemon.struct}
                ))
                val result = ConfigManager.CONFIG.playerLevelSpawnEquation?.let { runtime.resolveDouble(it) } ?: return@ifIsType
                event.entity.pokemon.level = result.toInt()
                var preEvo: PreEvolution? = event.entity.pokemon.species.preEvolution
                while(preEvo?.form?.preEvolution != null) {
                    preEvo = preEvo.form.preEvolution!!
                }
                if(preEvo != null) {
                    if (preEvo.form.evolutions.filterIsInstance<LevelUpEvolution>().any { !it.test(event.entity.pokemon) }) {
                        event.entity.pokemon.species = preEvo.species
                        event.entity.pokemon.form = preEvo.form
                        event.entity.pokemon.updateForm()
                        event.entity.pokemon.initialize()
                    }
                }
            }
        }
    }

    fun reload() {
        ConfigManager.load()
    }
}
