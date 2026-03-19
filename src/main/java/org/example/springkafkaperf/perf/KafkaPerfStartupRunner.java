package org.example.springkafkaperf.perf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class KafkaPerfStartupRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaPerfStartupRunner.class);

    private final RequestReplyPerfService requestReplyPerfService;

    public KafkaPerfStartupRunner(RequestReplyPerfService requestReplyPerfService) {
        this.requestReplyPerfService = requestReplyPerfService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        SyncPerfRunResult result = requestReplyPerfService.runSync();
        LOGGER.info("Kafka sync perf run completed: {}", result);
    }
}

