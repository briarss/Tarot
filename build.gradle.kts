@file:Suppress("UnstableApiUsage")

import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.function.Function


plugins {
    java
    idea
    id("fabric-loom") version ("1.7-SNAPSHOT")
    kotlin("jvm") version ("1.9.22")
    `maven-publish`
    id("com.gradleup.shadow") version ("8.3.5")
}
val modId = project.properties["mod_id"].toString()
version = project.properties["mod_version"].toString()
group = project.properties["mod_group"].toString()

val modName = project.properties["mod_name"].toString()
base.archivesName.set(modName)

val minecraftVersion = project.properties["minecraft_version"].toString()

loom {
    mixin.useLegacyMixinAp.set(true)
    interfaceInjection.enableDependencyInterfaceInjection.set(true)
    splitEnvironmentSourceSets()
    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
    if (file("src/main/resources/$modId.accesswidener").exists()) {
        accessWidenerPath.set(file("src/main/resources/$modId.accesswidener"))
    }
}
idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

val modImplementationInclude by configurations.register("modImplementationInclude")
val shade by configurations.register("shade") {
    isCanBeConsumed = false
    isCanBeResolved = true
}
configurations {
    modImplementationInclude
    shade
}

repositories {
    mavenCentral()
    mavenLocal()
    maven( "https://jitpack.io")
    maven("https://maven.parchmentmc.org")
    maven {
        name = "Modrinth"
        url = uri("https://api.modrinth.com/maven")
        content {
            includeGroup("maven.modrinth")
        }
    }
    maven("https://maven.nucleoid.xyz/") { name = "Nucleoid" }
    maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/") {
        name = "sonatype-oss-snapshots1"
        mavenContent { snapshotsOnly() }
    }
    maven("https://maven.ladysnake.org/releases") {
        name = "Ladysnake Mods"
    }
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://maven.impactdev.net/repository/development/")
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = URI("https://api.modrinth.com/maven")
            }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-1.21:${project.properties["parchment_version"]}")
    })

    modImplementation("net.fabricmc:fabric-loader:${project.properties["loader_version"].toString()}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.properties["fabric_version"].toString()}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${project.properties["fabric_kotlin_version"].toString()}")

    modImplementation(include("net.kyori:adventure-platform-fabric:5.14.1") {
        exclude("com.google.code.gson")
        exclude("ca.stellardrift", "colonel")
        exclude("net.fabricmc")
    })
    modCompileOnly("net.kyori:adventure-platform-mod-shared-fabric-repack:6.0.0")
    include("org.ladysnake.cardinal-components-api:cardinal-components-base:6.1.1")?.let {
        modImplementation(it)
    }
    include("org.ladysnake.cardinal-components-api:cardinal-components-entity:6.1.1")?.let {
        modImplementation(it)
    }

    // PermissionsAPI
    modImplementation("me.lucko:fabric-permissions-api:0.3.1")

    include("eu.pb4:sgui:1.6.1+1.21.1")?.let { modImplementation(it) }

    include("xyz.nucleoid:stimuli:0.4.12+1.21")?.let { modImplementation(it) }

    modImplementation("io.github.miniplaceholders:miniplaceholders-api:2.2.3")
    modImplementation("io.github.miniplaceholders:miniplaceholders-kotlin-ext:2.2.3")


    modImplementation("net.impactdev.impactor.api:economy:5.3.0-SNAPSHOT")
    modImplementation("net.impactdev.impactor.api:text:5.3.0-SNAPSHOT")

    modImplementation("com.cobblemon:fabric:1.6.1+1.21.1-SNAPSHOT")

    // Database Storage
    implementation(include("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")!!)
    shadow("org.mongodb:mongodb-driver-kotlin-coroutine:4.10.1")


    modImplementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    include("aster.amo.ceremony:Ceremony:2.0.2b")?.let {
        modImplementation(it)
    }
    include("com.github.shynixn.mccoroutine:mccoroutine-fabric-api:2.20.0")?.let {
        implementation(it)
    }
    include("com.github.shynixn.mccoroutine:mccoroutine-fabric-core:2.20.0")?.let {
        implementation(it)
    }
//
//    include("maven.modrinth:customnametags:0.3.0+1.21.3")?.let {
//        modImplementation(it)
//    }
}


tasks.processResources {
    inputs.property("version", version)

    filesMatching("fabric.mod.json") {
        expand("version" to version)
    }
}


publishing {
    publications.create<MavenPublication>("maven") {
        artifactId = base.archivesName.get()
        from(components["java"])
    }

    repositories {
        mavenLocal()
    }
}

tasks.processResources {
    inputs.property("mod_version", version)

    filesMatching("fabric.mod.json") {
        expand("id" to modId, "version" to version, "name" to modName)
    }

//    filesMatching("**/lang/*.json") {
//        expand("id" to modId, "version" to version, "name" to modName)
//    }
}

tasks {
    remapJar {
        archiveFileName.set("${project.name}-fabric-$minecraftVersion-${project.version}.jar")
        dependsOn(shadowJar)
        inputFile = file(shadowJar.get().archiveFile)
    }

    shadowJar {
        from("LICENSE")
        configurations = listOf(
            project.configurations.shadow.get()
        )
        archiveClassifier.set("dev-all")

        exclude("kotlin/**", "javax/**")
        exclude("org/checkerframework/**", "org/intellij/**", "org/jetbrains/annotations/**")
        exclude("com/google/gson/**")
        exclude("net/kyori/**")
        exclude("org/slf4j/**")

        val relocPath = "aster.amo.libs."
        relocate("com.mongodb", relocPath + "com.mongodb")
    }
}
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    version = JavaVersion.VERSION_21
    withSourcesJar()
}

tasks.withType<AbstractArchiveTask> {
    from("LICENSE") {
        rename { "${it}_${modId}" }
    }
}
tasks.create("hydrate") {
    doLast {
        val applyFileReplacements: Function<String, String> = Function { path ->
            path.replace("\$mod_name$", project.properties["mod_name"].toString())
                .replace("\$mod_id$", project.properties["mod_id"].toString())
                .replace("\$mod_group$", project.properties["mod_group"].toString())
        }
        val applyPathReplacements: Function<String, String> = Function { path ->
            path.replace("\$mod_name$", project.properties["mod_name"].toString())
                .replace("\$mod_id$", project.properties["mod_id"].toString())
                .replace("\$mod_group$", project.properties["mod_group"].toString().replace(".", "/"))
        }
        project.extensions.getByType<JavaPluginExtension>().sourceSets.forEach { sourceSet ->
            sourceSet.allSource.sourceDirectories.asFileTree.forEach { file ->
                val newPath = Paths.get(applyPathReplacements.apply(file.path))
                Files.createDirectories(newPath.parent)

                if (!file.path.endsWith(".png")) {
                    val lines =
                        Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)
                            .map { applyFileReplacements.apply(it) }
                    Files.deleteIfExists(file.toPath())
                    Files.write(
                        newPath,
                        lines
                    )
                } else {
                    Files.move(file.toPath(), newPath)
                }

                var parent = file.parentFile
                while (parent.listFiles()?.isEmpty() == true) {
                    Files.deleteIfExists(parent.toPath())
                    parent = parent.parentFile
                }
            }
        }
    }
}
