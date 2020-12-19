buildscript {
    repositories {
        jcenter()
        maven("https://plugins.gradle.org/m2/")
    }

    dependencies {
        val kotlinVersion = property("kotlin_version")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("org.jetbrains.kotlin:kotlin-serialization:$kotlinVersion")
        classpath("com.github.jengelman.gradle.plugins:shadow:6.1.0") // ... for packaging into a JAR
        classpath("com.github.gmazzo:gradle-buildconfig-plugin:2.0.2") // ... for "BuildConfig" object generation
    }
}

plugins {
    kotlin("jvm") version "1.4.21"
    kotlin("plugin.serialization") version "1.4.21"
    application
    id("com.github.gmazzo.buildconfig") version "2.0.2"
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

// BuildConfig with https://github.com/gmazzo/gradle-buildconfig-plugin .
buildConfig {
    val appVersionName = property("appVersionName")
    val appVersionCode = property("appVersionCode")

    buildConfigField("String", "versionName", "\"$appVersionName\"")
    buildConfigField("Int", "versionCode", "$appVersionCode")
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
    val kotlinVersion = property("kotlin_version")
    val ktorVersion = property("ktor_version")
    val logbackVersion = property("logback_version")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion") // Logger used by Ktor


    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")
    implementation("org.xerial:sqlite-jdbc:3.32.3.2") // SQLite DB driver
    implementation("net.coobird:thumbnailator:0.4.3") // For thumbnails
    implementation("com.drewnoakes:metadata-extractor:2.14.0") // For EXIF
    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")
}

val appVersionName = property("appVersionName")
val appVersionCode = property("appVersionCode")

setProperty("mainClassName", "jaaska.jaakko.photosapp.server.ApplicationKt")
group = "jaaska.jaakko"
version = "$appVersionName-$appVersionCode"
