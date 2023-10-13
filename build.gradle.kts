plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1" apply true
    id("xyz.jpenilla.run-paper") version "2.2.0"
}

repositories {
    mavenCentral()

    maven {
        name = "paper-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }

    maven {
        name = "glare-repo"
        url = uri("https://repo.glaremasters.me/repository/towny/")
    }

    maven {
        name = "codemc"
        url = uri("https://repo.codemc.io/repository/maven-snapshots/")
    }
}

dependencies {
    compileOnly(libs.paper)
    compileOnly(libs.towny)
    implementation(libs.anvilgui)
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
        minecraftVersion("1.20.2")

        downloadPlugins {
            github("TownyAdvanced", "Towny", "0.99.6.1", "towny-0.99.6.1.jar")

            // Non required plugins
            github("MilkBowl", "Vault", "1.7.3", "Vault.jar")
            github("iconomy5legacy", "iConomy", "5.21", "iConomy-5.21.jar")

            url("https://download.luckperms.net/1517/bukkit/loader/LuckPerms-Bukkit-5.4.104.jar")
        }
    }
}
