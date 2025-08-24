plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "jp.ertl.rfm"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://jitpack.io")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://repo.citizensnpcs.co/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    
    // WorldGuard
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.10")
    
    // Vault
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    
    // PlaceholderAPI
    compileOnly("me.clip:placeholderapi:2.11.6")
    
    // Citizens
    compileOnly("net.citizensnpcs:citizens-main:2.0.35-SNAPSHOT") {
        exclude(group = "*", module = "*")
    }
    compileOnly("net.citizensnpcs:citizensapi:2.0.35-SNAPSHOT")
    
    // DecentHolograms
    compileOnly("com.github.decentsoftware-eu:decentholograms:2.8.11")
    
    // Adventure (included in Paper)
    compileOnly("net.kyori:adventure-api:4.17.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.17.0")
    
    // Utilities
    implementation("org.yaml:snakeyaml:2.3")
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.mockito:mockito-core:5.14.2")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        minimize()
        
        relocate("org.yaml.snakeyaml", "jp.ertl.rfm.libs.snakeyaml")
    }
    
    build {
        dependsOn(shadowJar)
    }
    
    processResources {
        filesMatching("plugin.yml") {
            expand(mapOf("version" to project.version))
        }
    }
    
    test {
        useJUnitPlatform()
    }
}