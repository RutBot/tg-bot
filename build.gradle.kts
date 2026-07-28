plugins {
    kotlin("jvm") version "2.2.21" apply false
    kotlin("plugin.spring") version "2.0.21" apply false
    id("org.springframework.boot") version "3.3.5" apply false
    id("io.spring.dependency-management") version "1.1.6" apply false
}

allprojects {
    group = "ru.kruasanich"

    // Версия: gradle property `releaseVersion` → иначе `0.1.0-$BUILD_NUMBER-SNAPSHOT` → дефолт.
    val baseVersion = "0.1.0"
    val explicitVersion = providers.gradleProperty("releaseVersion").orNull
    val buildNumber = providers.environmentVariable("BUILD_NUMBER").orNull
    version = explicitVersion
        ?: buildNumber?.let { "$baseVersion-$it-SNAPSHOT" }
        ?: "$baseVersion-SNAPSHOT"
}

subprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.add("-Xjsr305=strict")
        }
    }
}
