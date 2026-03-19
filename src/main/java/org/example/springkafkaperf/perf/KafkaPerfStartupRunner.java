package org.example.springkafkaperf.perf;

import org.example.springkafkaperf.config.KafkaPerfProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class KafkaPerfStartupRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaPerfStartupRunner.class);

    private final KafkaSyncPerfService kafkaSyncPerfService;
    private final KafkaAsyncPerfService kafkaAsyncPerfService;
    private final KafkaClientsPerfService kafkaClientsPerfService;
    private final KafkaPerfProperties kafkaPerfProperties;

    public KafkaPerfStartupRunner(KafkaSyncPerfService kafkaSyncPerfService,
                                  KafkaAsyncPerfService kafkaAsyncPerfService,
                                  KafkaClientsPerfService kafkaClientsPerfService,
                                  KafkaPerfProperties kafkaPerfProperties) {
        this.kafkaSyncPerfService = kafkaSyncPerfService;
        this.kafkaAsyncPerfService = kafkaAsyncPerfService;
        this.kafkaClientsPerfService = kafkaClientsPerfService;
        this.kafkaPerfProperties = kafkaPerfProperties;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        KafkaPerfProperties.Mode mode = kafkaPerfProperties.getMode();
        PerfRunResult result = switch (mode) {
            case SYNC -> kafkaSyncPerfService.runSync();
            case DIRECT -> kafkaClientsPerfService.runDirect();
            case ASYNC -> kafkaAsyncPerfService.runAsync();
        };
        LOGGER.info("Kafka perf run completed for mode {}: {}", mode, result);
        System.exit(0);
    }
}

