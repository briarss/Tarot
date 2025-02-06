package aster.amo.tarot.config

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import aster.amo.tarot.Tarot
import aster.amo.tarot.schedulable.Schedulable
import aster.amo.tarot.utils.Utils
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
        Schedulable.schedulables.clear()
        Schedulable.schedulables.addAll(loadSchedulables(Tarot.INSTANCE.configDir.resolve("schedulables")))
        // optionally load a schedulables.json file and add all schedulables in the "schedulables" array to the list
        Schedulable.schedulables.addAll(loadSchedulablesFromJson(Tarot.INSTANCE.configDir.resolve("schedulables.json")))
    }

    private fun copyDefaults() {
        val classLoader = Tarot::class.java.classLoader

        Tarot.INSTANCE.configDir.mkdirs()

        attemptDefaultFileCopy(classLoader, "config.json")
    }

    fun loadSchedulables(directory: File): MutableList<Schedulable> {
        val schedulables = mutableListOf<Schedulable>()
        if (directory.exists() && directory.isDirectory) {
            loadConfigsRecursive<Schedulable>(directory, schedulables) { file ->
                try {
                    var path = file.toPath()
                    path = path.subpath(Tarot.INSTANCE.configDir.toPath().nameCount, path.nameCount)
                    val filter = loadFile(path.toString(), Schedulable())
                    filter
                } catch (e: Exception) {
                    Tarot.LOGGER.error("Error loading schedulable config from ${file.absolutePath}", e)
                    Schedulable()
                }
            }
        }
        return schedulables
    }

    private fun loadSchedulablesFromJson(file: File): List<Schedulable> {
        val schedulables = mutableListOf<Schedulable>()
        if (file.exists()) {
            try {
                FileReader(file).use { reader ->
                    val jsonElement = JsonParser.parseReader(reader)
                    if (jsonElement.isJsonObject) {
                        val jsonObject = jsonElement.asJsonObject
                        val jsonArray: JsonArray = jsonObject.getAsJsonArray("schedulables")
                        for (element: JsonElement in jsonArray) {
                            val schedulable = Tarot.INSTANCE.gsonPretty.fromJson(element, Schedulable::class.java)
                            schedulables.add(schedulable)
                        }
                    }
                }
            } catch (e: Exception) {
                Tarot.LOGGER.error("Error loading schedulables from ${file.absolutePath}", e)
            }
        }
        return schedulables
    }

    private fun <T> loadConfigsRecursive(directory: File, list: MutableList<T>, loadAction: (File) -> T) {
        directory.listFiles()?.forEach { file ->
            if (file.isFile && file.name.endsWith(".json")) {
                list.add(loadAction(file))
            } else if (file.isDirectory) {
                loadConfigsRecursive(file, list, loadAction)
            }
        }
    }

    private fun <K, V> loadConfigsRecursive(directory: File, map: MutableMap<K, V>, loadAction: (File) -> Pair<K, V>) {
        directory.listFiles()?.forEach { file ->
            if (file.isFile && file.name.endsWith(".json")) {
                val (key, value) = loadAction(file)
                map[key] = value
            } else if (file.isDirectory) {
                loadConfigsRecursive(file, map, loadAction)
            }
        }
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
                val stream = classLoader.getResourceAsStream("$assetPackage/$fileName")
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
                val sourceUrl = classLoader.getResource("$assetPackage/$directoryName")
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