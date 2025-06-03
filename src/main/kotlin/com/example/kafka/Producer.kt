package com.example.kafka

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.Header
import org.apache.kafka.common.header.internals.RecordHeader
import java.util.Properties

class Producer(bootstrapServers: String = "localhost:9092") {
    private val producer: KafkaProducer<String, String>

    init {
        val props = Properties()
        props["bootstrap.servers"] = bootstrapServers
        props["key.serializer"] = "org.apache.kafka.common.serialization.StringSerializer"
        props["value.serializer"] = "org.apache.kafka.common.serialization.StringSerializer"
        producer = KafkaProducer(props)
    }

    fun send(topic: String, key: String, value: String, headers: Iterable<Header>? = null) {
        val record = ProducerRecord<String, String>(topic, null, key, value, headers)
        producer.send(record)
    }

    fun close() {
        producer.close()
    }
}
