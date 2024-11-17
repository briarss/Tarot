package soul.software.tarot.config

import com.google.gson.stream.JsonReader
import aster.amo.tarot.Tarot
import soul.software.tarot.utils.Utils
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

object ConfigManager {
    private var assetPackage = "assets/${Tarot.MOD_ID}"

    lateinit var CONFIG: TarotConfig

    fun load() {
        // Load defaulted configs if they do not exist
        copyDefaults()

        // Load all files
        CONFIG = loadFile("config.json", TarotConfig())
    }

    private fun copyDefaults() {
        val classLoader = Tarot::class.java.classLoader

        Tarot.INSTANCE.configDir.mkdirs()

        attemptDefaultFileCopy(classLoader, "config.json")
    }

    fun <T : Any> loadFile(filename: String, default: T, create: Boolean = false): T {
        val file = File(Tarot.INSTANCE.configDir, filename)
        var value: T = default
        try {
            Files.createDirectories(Tarot.INSTANCE.configDir.toPath())
            if (file.exists()) {
                FileReader(file).use { reader ->
                    val jsonReader = JsonReader(reader)
                    value = Tarot.INSTANCE.gsonPretty.fromJson(jsonReader, default::class.java)
                }
            } else if (create) {
                Files.createFile(file.toPath())
                FileWriter(file).use { fileWriter ->
                    fileWriter.write(Tarot.INSTANCE.gsonPretty.toJson(default))
                    fileWriter.flush()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return value
    }

    fun <T> saveFile(filename: String, `object`: T): Boolean {
        val dir = Tarot.INSTANCE.configDir
        val file = File(dir, filename)
        try {
            FileWriter(file).use { fileWriter ->
                fileWriter.write(Tarot.INSTANCE.gsonPretty.toJson(`object`))
                fileWriter.flush()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    private fun attemptDefaultFileCopy(classLoader: ClassLoader, fileName: String) {
        val file = Tarot.INSTANCE.configDir.resolve(fileName)
        if (!file.exists()) {
            try {
                val stream = classLoader.getResourceAsStream("${assetPackage}/$fileName")
                    ?: throw NullPointerException("File not found $fileName")

                Files.copy(stream, file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            } catch (e: Exception) {
                Utils.printError("Failed to copy the default file '$fileName': $e")
            }
        }
    }

    private fun attemptDefaultDirectoryCopy(classLoader: ClassLoader, directoryName: String) {
        val directory = Tarot.INSTANCE.configDir.resolve(directoryName)
        if (!directory.exists()) {
            directory.mkdirs()
            try {
                val sourceUrl = classLoader.getResource("${assetPackage}/$directoryName")
                    ?: throw NullPointerException("Directory not found $directoryName")
                val sourcePath = Paths.get(sourceUrl.toURI())

                Files.walk(sourcePath).use { stream ->
                    stream.filter { Files.isRegularFile(it) }
                        .forEach { sourceFile ->
                            val destinationFile = directory.resolve(sourcePath.relativize(sourceFile).toString())
                            Files.copy(sourceFile, destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        }
                }
            } catch (e: Exception) {
                Utils.printError("Failed to copy the default directory '$directoryName': " + e.message)
            }
        }
    }
}
