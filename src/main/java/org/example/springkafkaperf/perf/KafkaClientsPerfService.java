package org.example.springkafkaperf.perf;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.example.springkafkaperf.config.KafkaPerfProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class KafkaClientsPerfService {

    private static final Duration POLL_INTERVAL = Duration.ofMillis(5);

    private final KafkaPerfProperties properties;
    private final String bootstrapServers;
    private final String producerAcks;
    private final int producerRetries;
    private final String consumerAutoOffsetReset;

    public KafkaClientsPerfService(KafkaPerfProperties properties,
                                   @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
                                   @Value("${spring.kafka.producer.acks:all}") String producerAcks,
                                   @Value("${spring.kafka.producer.retries:3}") int producerRetries,
                                   @Value("${spring.kafka.consumer.auto-offset-reset:latest}") String consumerAutoOffsetReset) {
        this.properties = properties;
        this.bootstrapServers = bootstrapServers;
        this.producerAcks = producerAcks;
        this.producerRetries = producerRetries;
        this.consumerAutoOffsetReset = consumerAutoOffsetReset;
    }

    public PerfRunResult runDirect() throws InterruptedException {
        return runDirect(properties.getMessageCount());
    }

    public PerfRunResult runDirect(int count) throws InterruptedException {
        long started = System.currentTimeMillis();
        int repliedCount = 0;
        int timeoutCount = 0;
        long totalRoundTripMs = 0L;
        long maxRoundTripMs = 0L;
        String runId = UUID.randomUUID().toString();

        try (KafkaProducer<String, String> requestProducer = new KafkaProducer<>(producerProperties());
             KafkaConsumer<String, String> replyConsumer = new KafkaConsumer<>(consumerProperties(replyConsumerGroup(runId)))) {
            replyConsumer.subscribe(Collections.singletonList(properties.getReplyTopicClient()));
            try {
                waitForReady(replyConsumer, properties.getTimeout(), "direct reply consumer");
            } catch (TimeoutException ex) {
                throw new IllegalStateException("Timed out waiting for the direct reply consumer to be assigned", ex);
            }

            DirectReplyWorker replyWorker = new DirectReplyWorker(runId);
            replyWorker.startAndAwaitReady();
            try {
                for (int i = 0; i < count; i++) {
                    String key = "client-" + runId + '-' + i;
                    ProducerRecord<String, String> record = new ProducerRecord<>(
                            properties.getRequestTopicClient(),
                            key,
                            payload(i)
                    );

                    long sentAtMs = System.currentTimeMillis();
                    try {
                        requestProducer.send(record).get(properties.getTimeout().toMillis(), TimeUnit.MILLISECONDS);
                        ConsumerRecord<String, String> replyRecord = awaitReply(replyConsumer, key);
                        if (replyRecord.value() != null) {
                            repliedCount++;
                            long roundTripMs = Math.max(System.currentTimeMillis() - sentAtMs, 0L);
                            totalRoundTripMs += roundTripMs;
                            maxRoundTripMs = Math.max(maxRoundTripMs, roundTripMs);
                        }
                        replyWorker.assertHealthy();
                    } catch (TimeoutException | ExecutionException ex) {
                        timeoutCount++;
                    }
                }
                replyWorker.assertHealthy();
            } finally {
                replyWorker.close();
                replyWorker.awaitTermination();
            }
        }

        long durationMs = Math.max(System.currentTimeMillis() - started, 0L);
        double averageRoundTripMs = repliedCount == 0 ? 0D : (double) totalRoundTripMs / repliedCount;

        return new PerfRunResult(
                count,
                repliedCount,
                timeoutCount,
                durationMs,
                averageRoundTripMs,
                maxRoundTripMs
        );
    }

    private ConsumerRecord<String, String> awaitReply(KafkaConsumer<String, String> replyConsumer,
                                                      String expectedKey) throws TimeoutException {
        long timeoutMs = properties.getTimeout().toMillis();
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);

        while (System.nanoTime() < deadline) {
            ConsumerRecords<String, String> records = replyConsumer.poll(POLL_INTERVAL);
            for (ConsumerRecord<String, String> record : records) {
                if (expectedKey.equals(record.key())) {
                    return record;
                }
            }
        }

        throw new TimeoutException("Timed out waiting for reply key " + expectedKey);
    }

    private void waitForReady(KafkaConsumer<String, String> consumer,
                              Duration timeout,
                              String consumerName) throws TimeoutException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (consumer.assignment().isEmpty() && System.nanoTime() < deadline) {
            consumer.poll(POLL_INTERVAL);
        }
        if (consumer.assignment().isEmpty()) {
            throw new TimeoutException("Timed out waiting for assignment for " + consumerName);
        }

        Set<TopicPartition> assignment = consumer.assignment();
        consumer.seekToEnd(assignment);
        for (TopicPartition topicPartition : assignment) {
            consumer.position(topicPartition);
        }
    }

    private Properties producerProperties() {
        Properties producerProperties = new Properties();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProperties.put(ProducerConfig.ACKS_CONFIG, producerAcks);
        producerProperties.put(ProducerConfig.RETRIES_CONFIG, producerRetries);
        return producerProperties;
    }

    private Properties consumerProperties(String groupId) {
        Properties consumerProperties = new Properties();
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumerAutoOffsetReset);
        consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return consumerProperties;
    }

    private String replyConsumerGroup(String runId) {
        return properties.getConsumerGroup() + "-client-reply-" + runId;
    }

    private String requestConsumerGroup(String runId) {
        return properties.getConsumerGroup() + "-client-request-" + runId;
    }

    private String payload(int index) {
        int payloadSize = Math.max(properties.getPayloadBytes(), 8);
        String base = "client-msg-" + index + '-';
        if (base.length() >= payloadSize) {
            return base.substring(0, payloadSize);
        }

        StringBuilder builder = new StringBuilder(payloadSize);
        builder.append(base);
        while (builder.length() < payloadSize) {
            builder.append('x');
        }
        return builder.toString();
    }

    private final class DirectReplyWorker implements AutoCloseable {

        private final AtomicBoolean running = new AtomicBoolean(true);
        private final CountDownLatch started = new CountDownLatch(1);
        private final Thread thread;
        private final String runId;

        private volatile KafkaConsumer<String, String> requestConsumer;
        private volatile Throwable failure;

        private DirectReplyWorker(String runId) {
            this.runId = runId;
            this.thread = new Thread(this::runLoop, "kafka-client-perf-reply-worker");
        }

        private void startAndAwaitReady() throws InterruptedException {
            thread.start();
            if (!started.await(properties.getTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException("Timed out starting the direct reply worker");
            }
            assertHealthy();
        }

        private void awaitTermination() throws InterruptedException {
            thread.join(properties.getTimeout().toMillis());
            if (thread.isAlive()) {
                thread.interrupt();
                thread.join(1_000L);
            }
            assertHealthy();
        }

        private void assertHealthy() {
            if (failure != null) {
                throw new IllegalStateException("Direct kafka-clients reply worker failed", failure);
            }
        }

        private void runLoop() {
            try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProperties(requestConsumerGroup(runId)));
                 KafkaProducer<String, String> producer = new KafkaProducer<>(producerProperties())) {
                this.requestConsumer = consumer;
                consumer.subscribe(Collections.singletonList(properties.getRequestTopicClient()));
                waitForReady(consumer, properties.getTimeout(), "direct request responder");
                started.countDown();

                while (running.get()) {
                    ConsumerRecords<String, String> records = consumer.poll(POLL_INTERVAL);
                    for (ConsumerRecord<String, String> record : records) {
                        ProducerRecord<String, String> replyRecord = new ProducerRecord<>(
                                properties.getReplyTopicClient(),
                                record.key(),
                                record.value()
                        );
                        producer.send(replyRecord).get(properties.getTimeout().toMillis(), TimeUnit.MILLISECONDS);
                    }
                }
            } catch (WakeupException ex) {
                if (running.get()) {
                    failure = ex;
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                if (running.get()) {
                    failure = ex;
                }
            } catch (Exception ex) {
                failure = ex;
            } finally {
                this.requestConsumer = null;
                started.countDown();
            }
        }

        @Override
        public void close() {
            running.set(false);
            KafkaConsumer<String, String> consumer = requestConsumer;
            if (consumer != null) {
                consumer.wakeup();
            }
        }
    }
}

