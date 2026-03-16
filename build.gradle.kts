plugins {
    java
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
	alias(libs.plugins.minotaur)
	alias(libs.plugins.mod.publish)
	alias(libs.plugins.hangar.publish)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")

	maven("https://repo.glaremasters.me/repository/towny/") {
		mavenContent { includeGroup("com.palmergames.bukkit.towny") }
	}

	maven("https://mvn.wesjd.net/") {
		mavenContent { includeGroup("net.wesjd") }
	}
}

dependencies {
    compileOnly(libs.paper)
    compileOnly(libs.towny)
    implementation(libs.anvilgui)
}

java.sourceCompatibility = JavaVersion.VERSION_21

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
        options.release.set(21)
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()

        expand("version" to project.version)
    }

    runServer {
        minecraftVersion("1.21.11")

        downloadPlugins {
			libs.versions.towny.map { github("TownyAdvanced", "Towny", it, "towny-${it}.jar") }

            // Non required plugins
            github("MilkBowl", "Vault", "1.7.3", "Vault.jar")
            github("iconomy5legacy", "iConomy", "5.21", "iConomy-5.21.jar")
        }
    }

	javadoc {
		val options = options as StandardJavadocDocletOptions

		options.use()
		options.links(
			"https://jd.papermc.io/paper/${libs.versions.paper.get().substringBefore("-")}/",
			"https://townyadvanced.github.io/Towny/javadoc/release/"
		)
	}
}

modrinth {
	token.set(System.getenv("MODRINTH_TOKEN"))
	projectId.set("townymenus")
	versionNumber.set(project.version as String)
	versionType.set("release")
	versionName.set("${project.name} ${versionNumber.get()}")
	uploadFile.set(tasks.jar.get().archiveFile)
	gameVersions.addAll((property("modrinthVersions") as String).split(",").map { it.trim() })
	loaders.addAll("paper", "folia")
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
		content.set("Version ${project.version} has been released \n${changelog.get()}")
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

				val versions: List<String> = (property("hangarVersions") as String)
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
	tasks.getByName("publishGithub").dependsOn(tasks.shadowJar)

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
	rootProject.file("src/main/resources/Changelog.txt").readLines().forEach { line ->
		if (line.startsWith(version))
			versionFound = true
		else if (versionFound && !line.trim().startsWith("-"))
			return@forEach

		if (versionFound && line.trim().startsWith("-"))
			lines.add(line)
	}

	return lines.joinToString("\n")
}
