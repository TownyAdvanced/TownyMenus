plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.2" apply true
    id("xyz.jpenilla.run-paper") version "1.0.6"
}

repositories {
    mavenCentral()

    // Spigot API
    maven {
        name = "spigot-repo"
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }

    // Towny
    maven {
        name = "glare-repo"
        url = uri("https://repo.glaremasters.me/repository/towny/")
    }

    // AnvilGUI
    maven {
        name = "codemc"
        url = uri("https://repo.codemc.io/repository/maven-snapshots/")
    }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.19.2-R0.1-SNAPSHOT")
    compileOnly("com.palmergames.bukkit.towny:towny:0.98.3.8")
    compileOnly("org.jetbrains:annotations:23.0.0")
    implementation("net.wesjd:anvilgui:1.5.3-SNAPSHOT")
}
group = "io.github.townyadvanced"
version = "0.0.1"
java.sourceCompatibility = JavaVersion.VERSION_17

tasks {
    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        archiveClassifier.set("")

        dependencies {
            include(dependency("net.wesjd:anvilgui"))
        }

        relocate("net.wesjd.anvilgui", "io.github.townyadvanced.townymenus.libs.anvilgui")
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(17)
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()

        expand("version" to project.version)
    }

    runServer {
        minecraftVersion("1.18.2")
    }
}

tasks.withType<JavaCompile> {
    options.encoding = Charsets.UTF_8.name()
}