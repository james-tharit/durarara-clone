package com.example.kafka

import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration
import java.util.Properties
import java.util.Collections

class Consumer {
    private val consumer: KafkaConsumer<String, String>

    init {
        val props = Properties()
        props["bootstrap.servers"] = "localhost:9092"
        props["key.deserializer"] = "org.apache.kafka.common.serialization.StringDeserializer"
        props["value.deserializer"] = "org.apache.kafka.common.serialization.StringDeserializer"
        props["group.id"] = "my-consumer-group"
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
