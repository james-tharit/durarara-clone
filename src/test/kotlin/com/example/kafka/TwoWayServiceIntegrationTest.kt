package com.example.kafka

import kafka.server.KafkaConfig
import kafka.tools.StreamsResetter
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.common.utils.Utils
import org.apache.kafka.test.EmbeddedKafkaCluster
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File
import java.time.Duration
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS) // To allow @BeforeAll and @AfterAll on non-static methods
class TwoWayServiceIntegrationTest {

    private lateinit var kafkaCluster: EmbeddedKafkaCluster
    private val requestTopic = "request-topic-integration"
    private val responseTopic = "response-topic-integration"
    private val correlationIdHeader = "correlationId"
    private var bootstrapServers: String = ""

    private lateinit var responderThread: Thread
    private volatile var responderRunning = true


    @BeforeAll
    fun setUpKafka() {
        // Hack to work around Kafka Test Utils classloading issues with Zookeeper by deleting the folder it creates
        // See KAFKA-16339 - this is a common problem with EmbeddedKafkaCluster if tests run multiple times or in certain environments
        val zkSnapshotDir = File("./zookeeper")
        if (zkSnapshotDir.exists()) {
            Utils.delete(zkSnapshotDir)
        }

        kafkaCluster = EmbeddedKafkaCluster(1, Properties().apply {
            // The EmbeddedKafkaCluster constructor may try to create the log dir if it doesn't exist
            // and might not have perms. Pre-creating it.
            val logDir = File("./kafka-logs")
            if (!logDir.exists()) logDir.mkdirs()
            setProperty(KafkaConfig.LogDirProp(), logDir.absolutePath)
        })
        kafkaCluster.start()
        bootstrapServers = kafkaCluster.bootstrapServers()

        val adminProperties = Properties()
        adminProperties[AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        AdminClient.create(adminProperties).use { adminClient ->
            val topics = listOf(
                NewTopic(requestTopic, 1, 1.toShort()),
                NewTopic(responseTopic, 1, 1.toShort())
            )
            adminClient.createTopics(topics).all().get()
        }

        startResponder()
    }

    private fun startResponder() {
        responderRunning = true
        val responderProps = Properties()
        responderProps[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        responderProps[ConsumerConfig.GROUP_ID_CONFIG] = "integration-test-responder-group"
        responderProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
        responderProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
        responderProps[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"

        val producerProps = Properties()
        producerProps[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        producerProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
        producerProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name

        responderThread = Thread {
            KafkaConsumer<String, String>(responderProps).use { consumer ->
                KafkaProducer<String, String>(producerProps).use { producer ->
                    consumer.subscribe(listOf(requestTopic))
                    while (responderRunning) {
                        val records = consumer.poll(Duration.ofMillis(100))
                        for (record in records) {
                            val correlationId = record.headers().lastHeader(correlationIdHeader)?.value()
                            assertNotNull(correlationId, "Correlation ID missing in request from TwoWayService")

                            val responseValue = "Response: ${record.value()}"
                            val responseRecord = ProducerRecord(
                                responseTopic,
                                record.key(),
                                responseValue
                            )
                            responseRecord.headers().add(RecordHeader(correlationIdHeader, correlationId))
                            producer.send(responseRecord)
                        }
                    }
                }
            }
        }
        responderThread.start()
    }


    @AfterAll
    fun tearDownKafka() {
        responderRunning = false
        if (::responderThread.isInitialized) {
            responderThread.interrupt()
            responderThread.join(5000) // Wait for responder to shut down
        }
        if (::kafkaCluster.isInitialized) {
            kafkaCluster.stop()
        }
        // Clean up Kafka log directories, critical for rerunning tests
        val logDir = File("./kafka-logs")
        if (logDir.exists()) {
            Utils.delete(logDir)
        }
        val zkSnapshotDir = File("./zookeeper")
        if (zkSnapshotDir.exists()) {
            Utils.delete(zkSnapshotDir)
        }
        // StreamsResetter can sometimes leave problematic state for ZK / Kafka, ensure clean for next run
        val streamsResetter = StreamsResetter()
        streamsResetter.run(arrayOf("--application-id", "kafka.tools.StreamsResetter", "--bootstrap-servers", bootstrapServers, "--force"))

    }

    private fun createTwoWayServiceInstance(): TwoWayService {
        // Modify Producer and Consumer to accept bootstrapServers externally for testing
        // For this test, we'll create new instances of producer and consumer with the test bootstrap servers
        // This is a simplification; a real app might use dependency injection or config files.

        val service = TwoWayService() // This will create its own producer/consumer with localhost:9092

        // We need to replace the internal producer and consumer with ones using the embedded cluster.
        // This is typically done with DI, but for now, reflection:
        val producerField = TwoWayService::class.java.getDeclaredField("producer")
        producerField.isAccessible = true
        val realProducer = producerField.get(service) as Producer
        realProducer.close() // Close the default producer

        val newTestProducer = Producer() // Create a new one
        val prodPropsField = newTestProducer.javaClass.getDeclaredField("producer")
        prodPropsField.isAccessible = true
        val kafkaProducerField = prodPropsField.get(newTestProducer).javaClass.getDeclaredField("producer") // KafkaProducer within Producer

        val newKafkaProducer = KafkaProducer<String,String>(Properties().apply {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
        })

        val producerInnerProducerField = newTestProducer.javaClass.getDeclaredField("producer")
        producerInnerProducerField.isAccessible = true
        (producerInnerProducerField.get(newTestProducer) as KafkaProducer<*, *>).close() // close existing internal kafka producer
        producerInnerProducerField.set(newTestProducer, newKafkaProducer) // set new one
        producerField.set(service, newTestProducer) // set modified producer to service


        val consumerField = TwoWayService::class.java.getDeclaredField("consumer")
        consumerField.isAccessible = true
        val realConsumer = consumerField.get(service) as Consumer
        realConsumer.close() // Close the default consumer

        val newTestConsumer = Consumer()
        val consumerPropsField = newTestConsumer.javaClass.getDeclaredField("consumer")
        consumerPropsField.isAccessible = true

        val newKafkaConsumer = KafkaConsumer<String,String>(Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
            put(ConsumerConfig.GROUP_ID_CONFIG, "integration-test-twoway-consumer-${UUID.randomUUID()}") // Unique group id
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest") // Don't read old messages from other tests
        })
        (consumerPropsField.get(newTestConsumer) as KafkaConsumer<*, *>).close() // close existing internal kafka consumer
        consumerPropsField.set(newTestConsumer, newKafkaConsumer) // set new one
        consumerField.set(service, newTestConsumer)

        // The TwoWayService starts its consumer thread in init. We need to restart it with the new consumer.
        // This is getting complex due to lack of DI.
        // For a real application, make bootstrap servers configurable for Producer/Consumer.
        // And allow injecting Producer/Consumer into TwoWayService.
        // For now, let's re-initialize the consumer part of TwoWayService.
        val responseTopicField = TwoWayService::class.java.getDeclaredField("responseTopic")
        responseTopicField.isAccessible = true
        val currentResponseTopic = responseTopicField.get(service) as String

        val correlationIdHeaderField = TwoWayService::class.java.getDeclaredField("correlationIdHeader")
        correlationIdHeaderField.isAccessible = true
        val currentCorrelationIdHeader = correlationIdHeaderField.get(service) as String

        val responseMapField = TwoWayService::class.java.getDeclaredField("responseMap")
        responseMapField.isAccessible = true
        val responseMap = responseMapField.get(service) as ConcurrentHashMap<String, String>

        val latchMapField = TwoWayService::class.java.getDeclaredField("latchMap")
        latchMapField.isAccessible = true
        val latchMap = latchMapField.get(service) as ConcurrentHashMap<String, CountDownLatch>


        Thread {
            newTestConsumer.subscribe(currentResponseTopic) { _, value, headers ->
                headers[currentCorrelationIdHeader]?.let { correlationId ->
                    responseMap[correlationId] = value
                    latchMap[correlationId]?.countDown()
                }
            }
        }.start()


        return service
    }


    @Test
    fun `sendAndReceive should get response via Kafka`() {
        val service = createTwoWayServiceInstance()
        val message = "Hello Integration Test"
        val expectedResponse = "Response: $message"

        val actualResponse = service.sendAndReceive(message)
        assertEquals(expectedResponse, actualResponse)

        service.close()
    }

    @Test
    fun `sendAndReceive should handle multiple messages correctly`() {
        val service = createTwoWayServiceInstance()
        val messages = listOf("Msg1", "Msg2", "Msg3", "Another Message", "Test 5")

        messages.forEach { msg ->
            val expectedResponse = "Response: $msg"
            val actualResponse = service.sendAndReceive(msg)
            assertEquals(expectedResponse, actualResponse, "Failed for message: $msg")
        }
        service.close()
    }
}
