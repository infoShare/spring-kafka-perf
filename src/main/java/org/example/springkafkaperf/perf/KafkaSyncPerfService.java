package org.example.springkafkaperf.perf;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.example.springkafkaperf.config.KafkaPerfProperties;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.requestreply.RequestReplyFuture;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class KafkaSyncPerfService {

    private final ReplyingKafkaTemplate<String, String, String> replyingKafkaTemplate;
    private final KafkaPerfProperties properties;

    public KafkaSyncPerfService(
            ReplyingKafkaTemplate<String, String, String> replyingKafkaTemplate,
            KafkaPerfProperties properties) {
        this.replyingKafkaTemplate = replyingKafkaTemplate;
        this.properties = properties;
    }

    public PerfRunResult runSync() throws InterruptedException {
        return runSync(properties.getMessageCount());
    }

    public PerfRunResult runSync(int count) throws InterruptedException {
        long started = System.currentTimeMillis();
        int repliedCount = 0;
        int timeoutCount = 0;
        long totalRoundTripMs = 0L;
        long maxRoundTripMs = 0L;

        for (int i = 0; i < count; i++) {
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    properties.getRequestTopicSync(),
                    "sync-" + i,
                    payload(i)
            );

            long sentAtMs = System.currentTimeMillis();
            try {
                RequestReplyFuture<String, String, String> future = replyingKafkaTemplate.sendAndReceive(record);
                ConsumerRecord<String, String> replyRecord = future.get(properties.getTimeout().toMillis(), TimeUnit.MILLISECONDS);
                if (replyRecord.value() != null) {
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
        String base = "sync-msg-" + index + "-";
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

