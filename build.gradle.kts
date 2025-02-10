import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val ktorVersion = "3.0.3" // Версия Ktor
val dotenvKotlinVersion = "6.5.0" // Версия dotenv-kotlin
val retrofitVersion = "2.11.0" // Версия Retrofit
val telegramBotVersion = "6.3.0" // Версия Kotlin Telegram Bot

plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
    application // Плагин для запуска Ktor-приложения
}

group = "io.pl"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    // Основные зависимости Ktor
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.github.cdimascio:dotenv-kotlin:$dotenvKotlinVersion")
    implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:$telegramBotVersion")

    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // Логирование
    implementation("ch.qos.logback:logback-classic:1.5.16")

    // JSON сериализация (например, Jackson)
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")

    // Тестирование
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.1.0")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// Указываем точку входа в приложение
application {
    mainClass.set("io.pl.ApplicationKt") // Имя файла с main-функцией
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "io.pl.ApplicationKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from({ configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) } })
}
