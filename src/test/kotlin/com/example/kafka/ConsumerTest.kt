package com.example.kafka

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.header.internals.RecordHeader
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import java.time.Duration
import java.util.Collections
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class ConsumerTest {

    @Mock
    private lateinit var mockKafkaConsumer: KafkaConsumer<String, String>

    @InjectMocks
    private lateinit var consumer: Consumer

    @Volatile // Ensure visibility across threads
    private var messageProcessed = false
    @Volatile
    private var receivedKey: String? = null
    @Volatile
    private var receivedValue: String? = null
    @Volatile
    private var receivedHeaders: Map<String, String>? = null


    @Test
    fun `subscribe should poll messages and process them`() {
        val topic = "test-topic"
        val key = "test-key"
        val value = "test-value"
        val headerKey = "correlationId"
        val headerValue = "abc"

        val record = ConsumerRecord(topic, 0, 0L, key, value)
        record.headers().add(RecordHeader(headerKey, headerValue.toByteArray()))
        val records = ConsumerRecords(mapOf(TopicPartition(topic, 0) to listOf(record)))

        `when`(mockKafkaConsumer.poll(any<Duration>()))
            .thenAnswer { // Simulate message consumption then empty polls
                if (!messageProcessed) {
                    records
                } else {
                    Thread.sleep(150) // prevent tight loop in test after first message
                    ConsumerRecords.empty()
                }
            }

        val onMessageReceivedCallback: (String, String, Map<String, String>) -> Unit = { k, v, h ->
            receivedKey = k
            receivedValue = v
            receivedHeaders = h
            messageProcessed = true
        }

        // Run subscribe in a separate thread because it has an infinite loop
        val consumerThread = Thread {
            try {
                consumer.subscribe(topic, onMessageReceivedCallback)
            } catch (e: InterruptedException) {
                // Expected when test finishes
                Thread.currentThread().interrupt()
            }
        }
        consumerThread.start()

        // Wait for the message to be processed or timeout
        var attempts = 0
        while (!messageProcessed && attempts < 100) { // Max 10 seconds wait
            Thread.sleep(100)
            attempts++
        }

        assertTrue(messageProcessed, "Message was not processed")
        assertEquals(key, receivedKey)
        assertEquals(value, receivedValue)
        assertEquals(headerValue, receivedHeaders?.get(headerKey))

        // Verify subscribe was called
        verify(mockKafkaConsumer).subscribe(argThat<List<String>> { topics -> topics.contains(topic) })

        // Interrupt the consumer thread to stop it
        consumerThread.interrupt()
        consumerThread.join(1000) // Wait for thread to finish

        // Close the consumer to release resources
        consumer.close()
        verify(mockKafkaConsumer).close()
    }
}
