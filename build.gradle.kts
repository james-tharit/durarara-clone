plugins {
    kotlin("jvm") version "1.9.22"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.apache.kafka:kafka-clients:3.7.0")
    testImplementation(kotlin("test"))
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.mockito:mockito-core:5.10.0")
    testImplementation("org.apache.kafka:kafka-clients:3.7.0:test") // For EmbeddedKafkaCluster
    testImplementation("org.slf4j:slf4j-simple:2.0.12") // SLF4J binding for Kafka test utils
}

application {
    mainClass.set("MainKt")
}
