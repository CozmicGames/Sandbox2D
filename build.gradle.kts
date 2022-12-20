import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
}

group = "com.cozmicgames"
version = "0.0.1"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(kotlin("reflect"))
    implementation("com.cozmicgames:Kore:0.1.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}