plugins {
    kotlin("jvm") version "2.2.21"
    id("io.ktor.plugin") version "3.3.2"
    id("org.openapi.generator") version "7.17.0"
    application
}

repositories {
    mavenCentral()
}

group = "com.lawrencejob"
version = "0.0.1"

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.lawrencejob.AppKt")
}

dependencies {
    implementation("io.ktor:ktor-server-core:3.3.2")
    implementation("io.ktor:ktor-server-netty:3.3.2")
    implementation("ch.qos.logback:logback-classic:1.4.14")

    testImplementation(kotlin("test"))
}

openApiGenerate {
    generatorName.set("kotlin-server")
    library.set("ktor")
    inputSpec.set("$projectDir/src/main/resources/openapi.yaml")
    outputDir.set("$buildDir/generated")
    packageName.set("com.lawrencejob.generated")
}