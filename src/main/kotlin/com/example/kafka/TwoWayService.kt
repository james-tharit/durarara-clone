package com.example.kafka

import org.apache.kafka.common.header.internals.RecordHeader
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class TwoWayService(private val producer: Producer, private val consumer: Consumer) {

    private val requestTopic = "request-topic"
    private val responseTopic = "response-topic"
    private val correlationIdHeader = "correlationId"

    private val responseMap = ConcurrentHashMap<String, String>()
    private val latchMap = ConcurrentHashMap<String, CountDownLatch>()

    // Keep track of the consumer thread to manage its lifecycle
    private var consumerThread: Thread? = null
    @Volatile private var isConsumerRunning = false

    init {
        startConsumerThread()
    }

    private fun startConsumerThread() {
        if (consumerThread?.isAlive == true) {
            return // Already running or starting
        }
        isConsumerRunning = true
        consumerThread = Thread {
            try {
                consumer.subscribe(responseTopic) { _, value, headers ->
                    if (!isConsumerRunning) return@subscribe // Stop processing if not running
                    headers[correlationIdHeader]?.let { correlationId ->
                        responseMap[correlationId] = value
                        latchMap[correlationId]?.countDown()
                    }
                }
            } catch (e: Exception) {
                if (isConsumerRunning) { // Don't log error if shutting down
                    // Log error, e.g., using a proper logger
                    System.err.println("Exception in consumer thread: ${e.message}")
                }
            } finally {
                // Potentially clean up resources if consumer loop exits unexpectedly
            }
        }
        consumerThread?.isDaemon = true // Allow JVM to exit if this is the only thread
        consumerThread?.start()
    }


    fun sendAndReceive(message: String): String {
        val correlationId = UUID.randomUUID().toString()
        val latch = CountDownLatch(1)
        latchMap[correlationId] = latch

        val headers = listOf(RecordHeader(correlationIdHeader, correlationId.toByteArray()))
        producer.send(requestTopic, "key", message, headers) // Assuming a generic key or null key

        // Wait for the response
        if (!latch.await(10, TimeUnit.SECONDS)) {
            latchMap.remove(correlationId) // Clean up
            throw RuntimeException("Timeout waiting for response with correlation ID: $correlationId")
        }

        latchMap.remove(correlationId)
        val response = responseMap.remove(correlationId)
            ?: throw IllegalStateException("Response not found even after latch countdown for correlation ID: $correlationId")

        return response
    }

    fun close() {
        isConsumerRunning = false // Signal consumer loop to stop processing new messages
        producer.close()
        // Attempt a more graceful shutdown for the consumer
        // The consumer.close() will interrupt the blocking poll() call.
        consumer.close()
        try {
            consumerThread?.interrupt() // Interrupt if it's stuck on something else (though poll should be interruptible)
            consumerThread?.join(5000) // Wait for the consumer thread to die
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            System.err.println("Interrupted while waiting for consumer thread to close.")
        }
    }
}
