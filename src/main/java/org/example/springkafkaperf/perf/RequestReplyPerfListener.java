package org.example.springkafkaperf.perf;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

@Component
public class RequestReplyPerfListener<R> {

    @KafkaListener(
            topics = "${kafka.perf.request-topic}",
            groupId = "${kafka.perf.consumer-group}-sync",
            containerFactory = "perfKafkaListenerContainerFactory"
    )
    @SendTo
    public String onRequest(String request) {
        return request;
    }
}

