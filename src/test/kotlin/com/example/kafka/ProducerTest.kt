package com.example.kafka

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.Header
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExtendWith(MockitoExtension::class)
class ProducerTest {

    @Mock
    private lateinit var mockKafkaProducer: KafkaProducer<String, String>

    @InjectMocks
    private lateinit var producer: Producer

    @Captor
    private lateinit var recordCaptor: ArgumentCaptor<ProducerRecord<String, String>>

    @Test
    fun `send should call kafkaProducer send with correct parameters`() {
        val topic = "test-topic"
        val key = "test-key"
        val value = "test-value"

        producer.send(topic, key, value)

        verify(mockKafkaProducer).send(recordCaptor.capture(), anyOrNull())

        val capturedRecord = recordCaptor.value
        assertEquals(topic, capturedRecord.topic())
        assertEquals(key, capturedRecord.key())
        assertEquals(value, capturedRecord.value())
        assertNull(capturedRecord.headers().lastHeader("correlationId"))
    }

    @Test
    fun `send with headers should call kafkaProducer send with correct parameters and headers`() {
        val topic = "test-topic"
        val key = "test-key"
        val value = "test-value"
        val headerKey = "correlationId"
        val headerValue = "123"
        val headers = listOf<Header>(object : Header {
            override fun key() = headerKey
            override fun value() = headerValue.toByteArray()
        })

        producer.send(topic, key, value, headers)

        verify(mockKafkaProducer).send(recordCaptor.capture(), anyOrNull())

        val capturedRecord = recordCaptor.value
        assertEquals(topic, capturedRecord.topic())
        assertEquals(key, capturedRecord.key())
        assertEquals(value, capturedRecord.value())
        assertEquals(headerValue, String(capturedRecord.headers().lastHeader(headerKey).value()))
    }
}
