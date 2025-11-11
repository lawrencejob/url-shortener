plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    id("io.ktor.plugin") version "3.3.2"
    application
}

repositories {
    mavenCentral()
}

group = "com.lawrencejob"
version = "0.0.1"

val koin_version = "4.1.1"

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.lawrencejob.AppKt")
}

dependencies {
    implementation("io.ktor:ktor-server-core:3.3.2")
    implementation("io.ktor:ktor-server-netty:3.3.2")
    implementation("io.ktor:ktor-server-content-negotiation:3.3.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.2")
    implementation("ch.qos.logback:logback-classic:1.4.14")

    implementation(project.dependencies.platform("io.insert-koin:koin-bom:$koin_version"))
    implementation("io.insert-koin:koin-core")
    implementation("io.insert-koin:koin-ktor")
    implementation("io.insert-koin:koin-logger-slf4j")

    implementation("io.lettuce:lettuce-core:6.7.1.RELEASE")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.10.2")

    implementation("io.viascom.nanoid:nanoid:1.0.1")

    implementation("com.typesafe:config:1.4.2")
    implementation("org.yaml:snakeyaml:2.2") // make it possible to read YAML config files (wow)

    testImplementation(kotlin("test"))
}
