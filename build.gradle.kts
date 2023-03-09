plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.0" apply true
    id("xyz.jpenilla.run-paper") version "2.0.1"
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

val townyVersion: String by project

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.19.3-R0.1-SNAPSHOT")
    compileOnly("com.palmergames.bukkit.towny:towny:${townyVersion}")
    compileOnly("org.jetbrains:annotations:24.0.1")
    implementation("net.wesjd:anvilgui:1.6.0-SNAPSHOT")
    annotationProcessor("com.github.bsideup.jabel:jabel-javac-plugin:1.0.0")
}

java.sourceCompatibility = JavaVersion.VERSION_17

tasks {
    assemble {
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
        minecraftVersion("1.19.3")
    }
}

tasks.withType<JavaCompile> {
    options.encoding = Charsets.UTF_8.name()

    // Configure source & release versions
    // https://github.com/bsideup/jabel
    sourceCompatibility = "17"
    options.release.set(8)

    javaCompiler.set(javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(17))
    })
}