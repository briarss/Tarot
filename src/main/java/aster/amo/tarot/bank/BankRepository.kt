package aster.amo.tarot.bank

import com.cobblemon.mod.common.pokemon.Pokemon
import com.mongodb.client.model.*
import com.mongodb.client.model.Indexes.ascending
import com.mongodb.kotlin.client.coroutine.MongoClient
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class BankRepository(val client: MongoClient) {
    private val database = client.getDatabase("tarot_bank")
    val collection = database.getCollection<Bank>("player_data")

    suspend fun init() {
        try {
            val caseInsensitiveCollation = Collation.builder()
                .locale("en")
                .caseLevel(false)
                .collationCaseFirst(CollationCaseFirst.OFF)
                .collationStrength(CollationStrength.SECONDARY)
                .build()

            collection.createIndex(
                ascending("player"),
                IndexOptions().unique(true).collation(caseInsensitiveCollation)
            )

            println("Indexes created successfully.")
        } catch (e: Exception) {
            println("Error initializing BankRepository: ${e.message}")
            throw e
        }
    }

    suspend fun writePlayerData(player: UUID, bank: Bank) {
        collection.replaceOne(Filters.eq("player", player.toString()), bank)
    }

    suspend fun readPlayerData(player: UUID): Bank? {
        return collection.find(Filters.eq("player", player.toString())).firstOrNull()
    }

    suspend fun addPokemonToBank(player: UUID, pokemon: Pokemon) {
        val bank = readPlayerData(player) ?: return
        bank.pokemon.add(pokemon)
        writePlayerData(player, bank)
    }

    suspend fun removePokemonFromBank(player: UUID, pokemon: Pokemon) {
        val bank = readPlayerData(player) ?: return
        bank.pokemon.remove(pokemon)
        writePlayerData(player, bank)
    }
}