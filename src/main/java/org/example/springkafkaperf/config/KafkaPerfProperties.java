package org.example.springkafkaperf.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "kafka.perf")
public class KafkaPerfProperties {

    private String requestTopic = "kafka-perf-request-topic";
    private String replyTopic = "kafka-perf-reply-topic";
    private String consumerGroup = "kafka-perf-group";
    private int messageCount = 1000;
    private int payloadBytes = 256;
    private Duration timeout = Duration.ofSeconds(30);
    private boolean startupEnabled;

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public String getRequestTopic() {
        return requestTopic;
    }

    public void setRequestTopic(String requestTopic) {
        this.requestTopic = requestTopic;
    }

    public String getReplyTopic() {
        return replyTopic;
    }

    public void setReplyTopic(String replyTopic) {
        this.replyTopic = replyTopic;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public int getPayloadBytes() {
        return payloadBytes;
    }

    public void setPayloadBytes(int payloadBytes) {
        this.payloadBytes = payloadBytes;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }


    public boolean isStartupEnabled() {
        return startupEnabled;
    }

    public void setStartupEnabled(boolean startupEnabled) {
        this.startupEnabled = startupEnabled;
    }
}

