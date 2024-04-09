plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("xyz.jpenilla.run-paper") version "2.2.3"
	id("com.modrinth.minotaur") version "2.8.7"
	id("me.modmuss50.mod-publish-plugin") version "0.5.1"
	id("io.papermc.hangar-publish-plugin") version "0.1.2"
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
    compileOnly(libs.spigot)
    compileOnly(libs.towny)
    compileOnly(libs.jetbrains.annotations)
    implementation(libs.anvilgui)
    annotationProcessor(libs.jabel)
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
        minecraftVersion("1.20.4")

        downloadPlugins {
            libs.towny.get().version?.let { github("TownyAdvanced", "Towny", it, "towny-${it}.jar") }

            // Non required plugins
            github("MilkBowl", "Vault", "1.7.3", "Vault.jar")
            github("iconomy5legacy", "iConomy", "5.21", "iConomy-5.21.jar")
        }
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

modrinth {
	token.set(System.getenv("MODRINTH_TOKEN"))
	projectId.set("townymenus")
	versionNumber.set(project.version as String)
	versionType.set("release")
	versionName.set("${project.name} ${versionNumber.get()}")
	uploadFile.set(tasks.jar.get().archiveFile)
	gameVersions.addAll((property("modrinthVersions") as String).split(",").map { it.trim() })
	loaders.addAll("bukkit", "paper", "folia")
	changelog.set(readChangelog())

	syncBodyFrom.set(rootProject.file("README.md").readText())
}

publishMods {
	file.set(tasks.jar.get().archiveFile)
	type.set(me.modmuss50.mpp.ReleaseType.STABLE)
	modLoaders.addAll("bukkit", "paper", "folia")

	changelog.set(readChangelog())

	github {
		accessToken.set(System.getenv("TOWNYMENUS_GITHUB_PAT"))
		repository.set("TownyAdvanced/TownyMenus")
		commitish.set("main")
		tagName.set(project.version as String)
		displayName.set("Version " + tagName.get())
	}

	discord {
		webhookUrl.set(System.getenv("TOWNYMENUS_DISCORD_WEBHOOK"))
		username.set("${project.name} Releases")
		content.set("Version ${project.version} has been released \n${changelog}")
	}
}

hangarPublish {
	publications.register("plugin") {
		version.set(project.version as String)
		channel.set("Release")
		id.set("TownyMenus")
		apiKey.set(System.getenv("HANGAR_API_TOKEN"))

		changelog.set(readChangelog())

		platforms {
			register(io.papermc.hangarpublishplugin.model.Platforms.PAPER) {
				jar.set(tasks.jar.get().archiveFile)

				val versions: List<String> = (property("paperVersions") as String)
					.split(",")
					.map { it.trim() }
				platformVersions.set(versions)

				dependencies {
					hangar("Towny") {
						required.set(true)
					}
				}
			}
		}
	}
}

tasks.register("publish") {
	if ((project.version as String).endsWith("-SNAPSHOT"))
		throw GradleException("Snapshot versions should not be deployed")

	tasks.getByName("publishPluginPublicationToHangar").dependsOn(tasks.shadowJar)

	dependsOn(tasks.publishMods)
	dependsOn(tasks.publishAllPublicationsToHangar)
	dependsOn(tasks.modrinth)

	//dependsOn(tasks.syncAllPagesToHangar)
	dependsOn(tasks.modrinthSyncBody)
}

tasks.register("readChangelog") {
	println(readChangelog())
}

fun readChangelog(): String {
	val lines = mutableListOf<String>()
	val version = project.version.toString().substringBefore("-") // remove -SNAPSHOT if present

	var versionFound = false
	rootProject.file("src/main/resources/Changelog.txt").readLines().forEach {
		val line = it.trim()

		if (line.startsWith(version))
			versionFound = true
		else if (versionFound && !line.startsWith("-"))
			return@forEach

		if (versionFound && line.startsWith("-"))
			lines.add(line)
	}

	return lines.joinToString("\n")
}
