package org.example.springkafkaperf.perf;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.example.springkafkaperf.config.KafkaPerfProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class KafkaAsyncPerfService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaPerfProperties properties;

    public KafkaAsyncPerfService(
            KafkaTemplate<String, String> kafkaTemplate,
            KafkaPerfProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    public PerfRunResult runAsync() throws InterruptedException {
        return runAsync(properties.getMessageCount());
    }

    public PerfRunResult runAsync(int count) throws InterruptedException {
        long started = System.currentTimeMillis();
        int repliedCount = 0;
        int timeoutCount = 0;
        long totalRoundTripMs = 0L;
        long maxRoundTripMs = 0L;

        for (int i = 0; i < count; i++) {
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    properties.getRequestTopicAsync(),
                    "async-" + i,
                    payload(i)
            );

            long sentAtMs = System.currentTimeMillis();
            try {
                CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(record);
                SendResult<String, String> results = future.get(properties.getTimeout().toMillis(), TimeUnit.MILLISECONDS);
                if (results != null) {
                    repliedCount++;
                    long roundTripMs = Math.max(System.currentTimeMillis() - sentAtMs, 0L);
                    totalRoundTripMs += roundTripMs;
                    maxRoundTripMs = Math.max(maxRoundTripMs, roundTripMs);
                }
            } catch (TimeoutException | ExecutionException ex) {
                timeoutCount++;
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

    private String payload(int index) {
        int payloadSize = Math.max(properties.getPayloadBytes(), 8);
        String base = "async-msg-" + index + "-";
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
}

