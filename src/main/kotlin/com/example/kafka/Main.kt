package com.example.kafka

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.isActive // Explicit import for isActive
import java.util.Properties

fun main() = runBlocking {
    val bootstrapServers = "localhost:9092"
    // Topics are already defined within TwoWayService, but good to be aware of them.
    // val requestTopic = "request-topic"
    // val responseTopic = "response-topic"

    // Note: Producer and Consumer default constructors already set these properties.
    // For a real application, externalizing this configuration is better.
    val producer = Producer() // Uses default localhost:9092
    val consumer = Consumer() // Uses default localhost:9092 and group.id

    val twoWayService = TwoWayService(producer, consumer)

    // The TwoWayService's init block already starts its consumer polling in a background thread.
    // No explicit start call for the consumer polling loop is needed here if TwoWayService handles it.

    println("Sending a message to Kafka...")
    val messageToSend = "Hello Kafka from Main!"

    // To simulate a separate responder service that would typically run independently
    // (like the one in our integration test), we'll launch a simple one here.
    // This responder will listen on "request-topic" and send to "response-topic".
    val responderJob = launch(Dispatchers.IO) {
        val responderConsumerProps = Properties().apply {
            put("bootstrap.servers", bootstrapServers)
            put("group.id", "main-responder-group")
            put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
            put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
            put("auto.offset.reset", "latest") // Process only new messages for this demo
        }
        val responderProducerProps = Properties().apply {
            put("bootstrap.servers", bootstrapServers)
            put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
            put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
        }

        val responderConsumer = org.apache.kafka.clients.consumer.KafkaConsumer<String, String>(responderConsumerProps)
        val responderProducer = org.apache.kafka.clients.producer.KafkaProducer<String, String>(responderProducerProps)

        responderConsumer.subscribe(listOf("request-topic"))
        println("Local responder started on 'request-topic'. Waiting for messages...")

        try {
            // Loop while the coroutine is active
            while (this.isActive) {
                val records = responderConsumer.poll(java.time.Duration.ofMillis(1000))
                // The poll() call is blocking but should be interrupted upon coroutine cancellation.
                // If records are empty, it just means no messages in this poll interval.
                // The loop condition `while (this.isActive)` handles cancellation.

                for (record in records) {
                    // Ensure we don't process if somehow we are no longer active after poll
                    if (!this.isActive) break

                    println("Responder received: ${record.value()} with correlationId: ${String(record.headers().lastHeader("correlationId").value())}")
                    val responseValue = "Echo: ${record.value()}"
                    val correlationIdHeader = record.headers().lastHeader("correlationId")

                    val responseRecord = org.apache.kafka.clients.producer.ProducerRecord(
                        "response-topic",
                        null,
                        record.key(),
                        responseValue,
                        listOf(correlationIdHeader)
                    )
                    responderProducer.send(responseRecord)
                    println("Responder sent: $responseValue to 'response-topic'")
                }
            }
        } catch (e: org.apache.kafka.common.errors.InterruptException) {
            println("Responder consumer interrupted.") // Expected on shutdown
        } finally {
            println("Responder closing.")
            responderConsumer.close()
            responderProducer.close()
        }
    }

    // Give the responder a moment to start up and subscribe
    kotlinx.coroutines.delay(3000)


    try {
        println("Main app: Attempting to send message: '$messageToSend'")
        val response = twoWayService.sendAndReceive(messageToSend)
        println("Main app: Received response: '$response'")

        // Send another message to test
        val messageToSend2 = "Another message from Main!"
        println("Main app: Attempting to send message: '$messageToSend2'")
        val response2 = twoWayService.sendAndReceive(messageToSend2)
        println("Main app: Received response: '$response2'")

    } catch (e: Exception) {
        System.err.println("An error occurred in main: ${e.message}")
        e.printStackTrace()
    } finally {
        println("Main app: Closing TwoWayService...")
        twoWayService.close() // This will also close the producer and consumer passed to it

        println("Main app: Stopping local responder...")
        responderJob.cancel() // Signal the responder coroutine to stop
        responderJob.join()   // Wait for the responder to finish cleaning up
        println("Main app: Responder stopped. Exiting.")
    }
}
