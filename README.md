# kafka-chat

## Overview
This project demonstrates a two-way communication service using Apache Kafka. It includes a producer to send messages, a consumer to receive messages, and a service that orchestrates request-response style communication over Kafka topics. The project is built with Kotlin and Gradle.

## How to Run Locally

### Prerequisites
- Java Development Kit (JDK) 11 or higher.
- Gradle (latest version recommended, or use the included Gradle wrapper `./gradlew`).
- Apache Kafka and Zookeeper running locally. For setup instructions, refer to the Kafka Quickstart guide: [https://kafka.apache.org/quickstart](https://kafka.apache.org/quickstart)

### Steps
1.  **Clone the repository:**
    ```bash
    git clone https://your-repository-url-here.git
    ```
    (Replace `https://your-repository-url-here.git` with the actual URL of this repository).

2.  **Navigate to the project directory:**
    ```bash
    cd kafka-chat
    ```
    (Or your specific project directory name).

3.  **Ensure Kafka and Zookeeper are running:**
    Follow the Kafka quickstart guide linked above to start Zookeeper and then the Kafka server. By default, Kafka usually runs on `localhost:9092`.

4.  **Create the required Kafka topics:**
    Open a new terminal in your Kafka installation directory.
    ```bash
    # For request-topic
    bin/kafka-topics.sh --create --topic request-topic --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1

    # For response-topic
    bin/kafka-topics.sh --create --topic response-topic --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
    ```
    Verify the topics are created:
    ```bash
    bin/kafka-topics.sh --list --bootstrap-server localhost:9092
    ```

5.  **Configure the application main class (if not already present):**
    Ensure your `build.gradle.kts` file has the `application` plugin and the `mainClass` property set. If you used the `application` plugin when setting up, it might already be there. If not, add or ensure the following block is present:
    ```kotlin
    // In build.gradle.kts
    plugins {
        kotlin("jvm") version "1.9.22" // Or your Kotlin version
        application // Ensure this plugin is applied
    }

    application {
        mainClass.set("com.example.kafka.MainKt") // Points to the Main.kt file
    }
    ```

6.  **Build and run the application:**
    Open a terminal in the root project directory and run:
    ```bash
    ./gradlew run
    ```
    This command will compile the Kotlin code and execute the `main` function in `src/main/kotlin/com/example/kafka/Main.kt`. You should see output indicating a message was sent and a response was received. The application will run until you manually stop it (e.g., with Ctrl+C) because of the active Kafka consumer.

## Project Structure
```
src
├── main
│   ├── kotlin
│   │   └── com/example/kafka/
│   │       ├── Main.kt         # Main application entry point
│   │       ├── Producer.kt     # Kafka message producer
│   │       ├── Consumer.kt     # Kafka message consumer
│   │       └── TwoWayService.kt # Service for request-response
│   └── resources
└── test
    ├── kotlin
    │   └── com/example/kafka/
    │       ├── ProducerTest.kt
    │       ├── ConsumerTest.kt
    │       ├── TwoWayServiceTest.kt
    │       └── TwoWayServiceIntegrationTest.kt
    └── resources
build.gradle.kts      # Gradle build script
README.md             # This file
```

## Next Steps
- Add comprehensive error handling and logging.
- Parameterize Kafka configurations (brokers, topics, group IDs) using a properties file or environment variables.
- Implement a more robust shutdown mechanism for the application and Kafka clients.