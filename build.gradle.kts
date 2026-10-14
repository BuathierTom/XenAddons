import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("net.fabricmc.fabric-loom")
    kotlin("jvm")
}

val mcVersion = sc.current.version
val modVersion = providers.gradleProperty("mod_version").get()
val archivesBaseName = providers.gradleProperty("archives_base_name").get()
val usesSharedSources = sc.current.isActive

version = "$modVersion-mc$mcVersion"
group = providers.gradleProperty("maven_group").get()

base {
    archivesName.set(archivesBaseName)
}

repositories {
    mavenCentral()
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1") {
        content {
            includeGroup("me.djtheredstoner")
        }
    }
}

loom {
    splitEnvironmentSourceSets()

    mods {
        register("xenaddons") {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["client"])
        }
    }

    runs {
        named("client") {
            generateRunConfig.set(true)
            runDirectory.set(layout.projectDirectory.dir("run"))
            jvmArguments.addAll(
                "-Ddevauth.enabled=${providers.gradleProperty("devauth_enabled").orElse("true").get()}",
                "-XX:+AllowEnhancedClassRedefinition",
                "-XX:+IgnoreUnrecognizedVMOptions",
            )
            providers.gradleProperty("devauth_account").orNull?.let {
                jvmArguments.add("-Ddevauth.account=$it")
            }
        }

        named("server") {
            generateRunConfig.set(false)
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")

    implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
    implementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")

    runtimeOnly("me.djtheredstoner:DevAuth-fabric:${property("devauth_version")}")

    val lwjglVersion = property("minecraft_lwjgl_version")
    implementation("org.lwjgl:lwjgl-nanovg:$lwjglVersion")
    include("org.lwjgl:lwjgl-nanovg:$lwjglVersion")

    listOf("windows", "linux", "macos", "macos-arm64").forEach { os ->
        implementation("org.lwjgl:lwjgl-nanovg:$lwjglVersion:natives-$os")
        include("org.lwjgl:lwjgl-nanovg:$lwjglVersion:natives-$os")
    }
}

tasks {
    processResources {
        val properties = mapOf(
            "version" to modVersion,
            "minecraft_dependency" to project.property("minecraft_dependency").toString(),
        )

        inputs.properties(properties)

        filesMatching("fabric.mod.json") {
            expand(properties)
        }
    }

    withType<KotlinCompile>().configureEach {
        inputs.property("stonecutterUsesSharedSources", usesSharedSources)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_25)
        }
    }

    withType<JavaCompile>().configureEach {
        options.release.set(25)
        options.encoding = "UTF-8"
    }

    jar {
        val licenseName = archivesBaseName
        from(rootProject.file("LICENSE")) {
            rename { "${it}_$licenseName" }
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

kotlin {
    jvmToolchain(25)
}
