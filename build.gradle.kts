plugins {
    id("java-library")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
    id("com.gradleup.shadow") version "9.5.1"
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("io.freefair.lombok") version "9.5.0"
}

repositories {
    mavenCentral()
    maven {
        name = "CodeMC"
        url = uri("https://repo.codemc.io/repository/maven-public")
    }
    maven { url = uri("https://maven.pvphub.me/tofaa") }
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    paperweight.paperDevBundle("26.2.build.+")
    compileOnly(files("libs/Jarona-0.1.0-all.jar"))
    compileOnly("de.tr7zw:item-nbt-api-plugin:2.15.7")
    compileOnly("io.github.alexdev03:unlimitednametags-api-paper:2.0.0")
    compileOnly("io.github.tofaa2:spigot:3.0.3-SNAPSHOT")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("26.2")
        jvmArgs("-Xms2G", "-Xmx2G", "-Dcom.mojang.eula.agree=true")
        pluginJars(files("libs/Jarona-0.1.0-all.jar"))
        downloadPlugins {
            modrinth("packetevents", "2.13.0+spigot")
            modrinth("nbtapi", "2.15.7")
        }
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }
}
