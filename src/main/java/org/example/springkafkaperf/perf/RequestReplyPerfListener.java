package org.example.springkafkaperf.perf;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

@Component
public class RequestReplyPerfListener<R> {

    @KafkaListener(
            topics = "${kafka.perf.request-topic-sync}",
            groupId = "${kafka.perf.consumer-group}",
            containerFactory = "perfKafkaListenerContainerFactory"
    )
    @SendTo
    public String onRequestSync(String request) {
        return request;
    }


    @KafkaListener(
            topics = "${kafka.perf.request-topic-async}",
            groupId = "${kafka.perf.consumer-group}",
            containerFactory = "perfKafkaListenerContainerFactory"
    )
    public void onRequestAsync(String request) {
        //Do nothing
    }
}

