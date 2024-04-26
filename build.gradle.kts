plugins {
    kotlin("jvm") version "1.9.23"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.google.cloud:google-cloud-vision:3.39.0")
    implementation("org.apache.pdfbox:pdfbox:2.0.31")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}