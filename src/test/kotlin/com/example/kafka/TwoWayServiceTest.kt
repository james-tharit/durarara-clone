package com.example.kafka

import org.apache.kafka.common.header.Header
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class TwoWayServiceTest {

    @Mock
    private lateinit var mockProducer: Producer

    @Mock
    private lateinit var mockConsumer: Consumer // We'll mock the behavior of the consumer

    private lateinit var twoWayService: TwoWayService

    @Captor
    private lateinit var stringCaptor: ArgumentCaptor<String>

    @Captor
    private lateinit var headersCaptor: ArgumentCaptor<Iterable<Header>>

    private var consumerCallback: ((key: String, value: String, headers: Map<String, String>) -> Unit)? = null

    private val executor = Executors.newSingleThreadExecutor()

    @BeforeEach
    fun setUp() {
        // Capture the callback passed to consumer.subscribe
        doAnswer { invocation ->
            consumerCallback = invocation.getArgument(1)
            null
        }.`when`(mockConsumer).subscribe(eq("response-topic"), any())

        // Use reflection to set the mock producer and consumer
        twoWayService = TwoWayService()
        val producerField = TwoWayService::class.java.getDeclaredField("producer")
        producerField.isAccessible = true
        producerField.set(twoWayService, mockProducer)

        val consumerField = TwoWayService::class.java.getDeclaredField("consumer")
        consumerField.isAccessible = true
        consumerField.set(twoWayService, mockConsumer)

        // Manually trigger the init block's consumer subscription logic
        // because the mockConsumer.subscribe won't be called by the real constructor
        // The TwoWayService constructor starts a thread that calls consumer.subscribe.
        // We need to simulate this behavior for the test.
         val initThread = Thread {
            consumerCallback?.let { /* Consumer already started by service, just ensure callback is captured */ }
            // Wait for callback to be captured by mockConsumer.subscribe
            var attempts = 0
            while(consumerCallback == null && attempts < 100){
                Thread.sleep(10)
                attempts++
            }
            if(consumerCallback == null) throw IllegalStateException("Consumer callback not captured")
        }
        initThread.start()
        initThread.join()


    }

    @AfterEach
    fun tearDown() {
        twoWayService.close() // Ensure resources are released
        executor.shutdownNow()
    }


    @Test
    fun `sendAndReceive should send request and receive response`() {
        val requestMessage = "Hello Kafka"
        val responseMessage = "Hello Back"

        // Simulate consumer receiving a message
        doAnswer { invocation ->
            // Simulate receiving a response by invoking the captured callback
            // This needs to be done in a separate thread to mimic the real consumer behavior
            executor.submit {
                Thread.sleep(100) // Short delay to simulate network latency
                val headers = headersCaptor.value.associate { it.key() to String(it.value()) }
                consumerCallback?.invoke("response-key", responseMessage, headers)
            }
            null
        }.`when`(mockProducer).send(eq("request-topic"), anyOrNull(), eq(requestMessage), headersCaptor.capture())


        val actualResponse = twoWayService.sendAndReceive(requestMessage)

        assertEquals(responseMessage, actualResponse)
        verify(mockProducer).send(eq("request-topic"), anyOrNull(), eq(requestMessage), any())
        // The consumer's subscribe is called in init, not directly asserting here for that.
    }

    @Test
    fun `sendAndReceive should timeout if no response`() {
        val requestMessage = "Hello Kafka"

        // No response will be simulated from the consumer for this test

        val exception = assertThrows<RuntimeException> {
            twoWayService.sendAndReceive(requestMessage)
        }

        assertEquals("Timeout waiting for response with correlation ID: ${
            headersCaptor.value.first { it.key() == "correlationId" }.let { String(it.value())}
        }", exception.message)
        verify(mockProducer).send(eq("request-topic"), anyOrNull(), eq(requestMessage), headersCaptor.capture())
    }

    @Test
    fun `sendAndReceive should handle multiple requests with correct correlation`() {
        val requestMessage1 = "Message 1"
        val responseMessage1 = "Response 1"
        val requestMessage2 = "Message 2"
        val responseMessage2 = "Response 2"

        val receivedCorrelationIds = mutableListOf<String>()


        // Simulate responses out of order or with delay
        doAnswer { invocation ->
            val headersIterable = invocation.getArgument<Iterable<Header>>(3)
            val currentCorrelationId = String(headersIterable.first { it.key() == "correlationId" }.value())
            receivedCorrelationIds.add(currentCorrelationId)

            val msg = invocation.getArgument<String>(2)

            executor.submit {
                if (msg == requestMessage1) {
                    Thread.sleep(200) // Respond to message 1 later
                    consumerCallback?.invoke("key1", responseMessage1, mapOf("correlationId" to currentCorrelationId))
                } else if (msg == requestMessage2) {
                    Thread.sleep(50)  // Respond to message 2 sooner
                    consumerCallback?.invoke("key2", responseMessage2, mapOf("correlationId" to currentCorrelationId))
                }
            }
            null
        }.`when`(mockProducer).send(eq("request-topic"), anyOrNull(), any(), any())


        // Send requests in parallel to test concurrency handling
        val future1 = executor.submit<String> {
            twoWayService.sendAndReceive(requestMessage1)
        }
        val future2 = executor.submit<String> {
            // Small delay to ensure sendAndReceive for msg1 likely starts its wait first
            // And its correlationID is captured first if send is not blocking.
            Thread.sleep(10)
            twoWayService.sendAndReceive(requestMessage2)
        }

        val actualResponse2 = future2.get(15, TimeUnit.SECONDS)
        val actualResponse1 = future1.get(15, TimeUnit.SECONDS)


        assertEquals(responseMessage1, actualResponse1)
        assertEquals(responseMessage2, actualResponse2)

        verify(mockProducer).send(eq("request-topic"), anyOrNull(), eq(requestMessage1), argThat { headers -> String(headers.first().value()) == receivedCorrelationIds[0] })
        verify(mockProducer).send(eq("request-topic"), anyOrNull(), eq(requestMessage2), argThat { headers -> String(headers.first().value()) == receivedCorrelationIds[1] })
    }
}
