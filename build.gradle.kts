plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.2" apply true
}

repositories {
    mavenCentral()
    maven {
        name = "paper-repo"
        url = uri("https://papermc.io/repo/repository/maven-public/")
    }

    maven {
        name = "glare-repo"
        url = uri("https://repo.glaremasters.me/repository/towny/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.18.2-R0.1-SNAPSHOT")
    compileOnly("com.palmergames.bukkit.towny:towny:0.98.1.0")
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
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(17)
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()

        expand("version" to project.version)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = Charsets.UTF_8.name()
}