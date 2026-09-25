plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":core"))

    // Spring Boot — только контейнер и автоконфигурация, без web/JPA: боту нужен long-polling.
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    // Telegram transport (telegrambots 7.x: модульный API).
    implementation("org.telegram:telegrambots-longpolling:7.2.0")
    implementation("org.telegram:telegrambots-client:7.2.0")
    implementation("org.telegram:telegrambots-meta:7.2.0")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation(kotlin("test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}

kotlin {
    jvmToolchain(21)
}

// Приложению нужен только исполняемый bootJar; отключаем «plain» jar,
// чтобы в build/libs (и в Docker COPY) не попадал второй артефакт.
tasks.named<Jar>("jar") {
    enabled = false
}

tasks.test {
    useJUnitPlatform()
}
