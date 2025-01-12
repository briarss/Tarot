package aster.amo.tarot.bank

import aster.amo.tarot.Tarot
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.asUUID
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext

class BankCodec : Codec<Bank> {
    override fun encode(writer: BsonWriter, value: Bank, encoderContext: EncoderContext) {
        writer.writeStartDocument()
        writer.writeName("player")
        writer.writeString(value.player.toString())
        writer.writeName("bank")
        writer.writeStartArray()
        value.pokemon.forEach {
            writer.writeStartDocument()
            val encodedPokemon = it.saveToJSON(Tarot.INSTANCE.server!!.registryAccess(), JsonObject())
            writer.writeString(it.uuid.toString(), encodedPokemon.toString())
            writer.writeEndDocument()
        }
        writer.writeEndArray()
        writer.writeEndDocument()
    }

    override fun getEncoderClass(): Class<Bank> {
        return Bank::class.java
    }

    override fun decode(reader: BsonReader, decoderContext: DecoderContext): Bank {
        reader.readStartDocument()
        val player = reader.readString("player").asUUID!!
        val bank = mutableListOf<Pokemon>()
        reader.readStartArray()
        while (reader.readBsonType() != null) {
            reader.readStartDocument()
            val pokemonString = reader.readString()
            val pokemon = Pokemon.loadFromJSON(Tarot.INSTANCE.server!!.registryAccess(), JsonParser.parseString(pokemonString).asJsonObject)
            bank.add(pokemon)
            reader.readEndDocument()
        }
        reader.readEndArray()
        reader.readEndDocument()
        return Bank(player, bank)
    }
}