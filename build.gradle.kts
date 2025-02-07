import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val ktorVersion = "3.0.3" // Версия Ktor

plugins {
    kotlin("jvm") version "2.1.0"
    application // Плагин для запуска Ktor-приложения
}

group = "io.pl"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    // Основные зависимости Ktor
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")

    // Логирование
    implementation("ch.qos.logback:logback-classic:1.4.11")

    // JSON сериализация (например, Jackson)
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")

    // Тестирование
    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")
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