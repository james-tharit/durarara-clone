package com.example.kafka

import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration
import java.util.Properties
import java.util.Collections

class Consumer(
    bootstrapServers: String = "localhost:9092",
    groupId: String = "my-consumer-group"
) {
    private val consumer: KafkaConsumer<String, String>

    init {
        val props = Properties()
        props["bootstrap.servers"] = bootstrapServers
        props["key.deserializer"] = "org.apache.kafka.common.serialization.StringDeserializer"
        props["value.deserializer"] = "org.apache.kafka.common.serialization.StringDeserializer"
        props["group.id"] = groupId
        // Add auto.offset.reset config to avoid issues with re-running tests or joining existing groups
        props["auto.offset.reset"] = "latest" // Or "earliest" depending on desired behavior
        consumer = KafkaConsumer(props)
    }

    fun subscribe(topic: String, onMessageReceived: (key: String, value: String, headers: Map<String, String>) -> Unit) {
        consumer.subscribe(Collections.singletonList(topic))
        while (true) {
            val records = consumer.poll(Duration.ofMillis(100))
            for (record in records) {
                val headersMap = record.headers().associate { it.key() to String(it.value()) }
                onMessageReceived(record.key(), record.value(), headersMap)
            }
        }
    }

    fun close() {
        consumer.close()
    }
}
