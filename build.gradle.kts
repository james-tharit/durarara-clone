plugins {
    kotlin("jvm") version "1.9.22"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0") // Added for coroutines
    implementation("org.apache.kafka:kafka-clients:3.7.0")
    testImplementation(kotlin("test")) // Provides basic Kotlin test annotations and JUnit5/Jupiter (should be transitive)
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")    // Explicit JUnit Jupiter API
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2") // Explicit JUnit Jupiter Engine (runtime)
    testImplementation("org.mockito:mockito-core:5.10.0")              // Core Mockito
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")    // Kotlin helpers for Mockito
    testImplementation("org.mockito:mockito-junit-jupiter:5.10.0") // Mockito integration with JUnit 5
    testImplementation(group = "org.apache.kafka", name = "kafka-clients", version = "3.7.0", classifier = "test") // For EmbeddedKafkaCluster
    testImplementation("org.apache.kafka:kafka_2.13:3.7.0") // For KafkaConfig
    testImplementation("org.apache.kafka:kafka-streams-test-utils:3.7.0") // For StreamsResetter
    testImplementation("org.scala-lang:scala-library:2.13.12") // Explicit Scala library
    testImplementation("org.slf4j:slf4j-simple:2.0.12") // SLF4J binding for Kafka test utils
}

application {
    mainClass.set("com.example.kafka.MainKt")
}

configurations.testImplementation.get().resolutionStrategy {
    force("org.apache.kafka:kafka-clients:3.7.0:test")
}
