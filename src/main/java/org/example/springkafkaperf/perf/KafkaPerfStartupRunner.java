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
    private final KafkaPerfProperties kafkaPerfProperties;

    public KafkaPerfStartupRunner(KafkaSyncPerfService kafkaSyncPerfService,
                                  KafkaAsyncPerfService kafkaAsyncPerfService,
                                  KafkaPerfProperties kafkaPerfProperties) {
        this.kafkaSyncPerfService = kafkaSyncPerfService;
        this.kafkaAsyncPerfService = kafkaAsyncPerfService;
        this.kafkaPerfProperties = kafkaPerfProperties;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        PerfRunResult result;
        if (kafkaPerfProperties.isSync()) {
            result = kafkaSyncPerfService.runSync();
        } else {
            result = kafkaAsyncPerfService.runAsync();
        }
        LOGGER.info("Kafka perf run completed: {} for sync {}", result, kafkaPerfProperties.isSync());
        System.exit(0);
    }
}

