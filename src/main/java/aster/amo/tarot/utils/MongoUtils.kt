package aster.amo.tarot.utils

import aster.amo.tarot.bank.BankCodec
import com.google.gson.Gson
import com.mongodb.MongoClientSettings
import com.mongodb.kotlin.client.coroutine.MongoClient
import org.bson.UuidRepresentation
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.configuration.CodecRegistry
import org.bson.codecs.pojo.PojoCodecProvider

object MongoUtils {

    fun createMongoClient(gson: Gson): MongoClient {
        val customCodecRegistry: CodecRegistry = CodecRegistries.fromRegistries(
            CodecRegistries.fromCodecs(BankCodec()),
            MongoClientSettings.getDefaultCodecRegistry(),
            CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build())
        )

        val settings = MongoClientSettings.builder()
            .codecRegistry(customCodecRegistry)
            .uuidRepresentation(UuidRepresentation.STANDARD)
            .build()
        return MongoClient.create(settings)
    }
}