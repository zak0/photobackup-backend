buildscript {
    repositories {
        jcenter()
        maven("https://plugins.gradle.org/m2/")
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin")
        classpath("org.jetbrains.kotlin:kotlin-serialization")
        classpath("com.github.jengelman.gradle.plugins:shadow") // ... for packaging into a JAR
        classpath("com.github.gmazzo:gradle-buildconfig-plugin") // ... for "BuildConfig" object generation
    }
}

plugins {
    kotlin("jvm") version Versions.kotlin
    kotlin("plugin.serialization") version Versions.kotlin
    application
    id("com.github.gmazzo.buildconfig") version "2.0.2"
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

// BuildConfig with https://github.com/gmazzo/gradle-buildconfig-plugin .
buildConfig {
    buildConfigField("String", "versionName", "\"${Versions.appVersionName}\"")
    buildConfigField("Int", "versionCode", "${Versions.appVersionCode}")
}

sourceSets {
    main {
        java {
            srcDirs("src", "build/generated")
        }

        resources {
            srcDirs("resources")
        }
    }

    test {
        java {
            srcDirs("test")
        }

        resources {
            srcDirs("testresources")
        }
    }
}

repositories {
    mavenLocal()
    jcenter()
    maven("https://kotlin.bintray.com/ktor")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}")
    implementation("ch.qos.logback:logback-classic:${Versions.logback}") // Logger used by Ktor

    implementation("io.ktor:ktor-server-core:${Versions.ktor}")
    implementation("io.ktor:ktor-server-auth:${Versions.ktor}")
    implementation("io.ktor:ktor-server-netty:${Versions.ktor}")
    implementation("io.ktor:ktor-server-content-negotiation:${Versions.ktor}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${Versions.ktor}")

    implementation("org.xerial:sqlite-jdbc:3.32.3.2") // SQLite DB driver
    implementation("net.coobird:thumbnailator:0.4.3") // For thumbnails of pictures
    implementation("org.jcodec:jcodec:0.2.5") // For thumbnails of videos
    implementation("org.jcodec:jcodec-javase:0.2.5") // For thumbnails of videos
    implementation("com.drewnoakes:metadata-extractor:2.14.0") // For EXIF

    testImplementation("io.ktor:ktor-server-tests:${Versions.ktor}")
}

// Below are properties for the assembled JAR
setProperty("mainClassName", "jaaska.jaakko.photosapp.server.ApplicationKt")
group = "jaaska.jaakko"
version = "${Versions.appVersionName}-${Versions.appVersionCode}"
