package org.example.springkafkaperf.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "kafka.perf")
public class KafkaPerfProperties {

    private String requestTopicSync = "kafka-perf-request-topic-sync";
    private String requestTopicAsync = "kafka-perf-request-topic-async";
    private String replyTopic = "kafka-perf-reply-topic";
    private String consumerGroup = "kafka-perf-group";
    private int messageCount = 1000;
    private int payloadBytes = 256;
    private Duration timeout = Duration.ofSeconds(30);
    private boolean sync;

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public String getRequestTopicSync() {
        return requestTopicSync;
    }

    public void setRequestTopicSync(String requestTopicSync) {
        this.requestTopicSync = requestTopicSync;
    }

    public String getRequestTopicAsync() {
        return requestTopicAsync;
    }

    public void setRequestTopicAsync(String requestTopicAsync) {
        this.requestTopicAsync = requestTopicAsync;
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


    public boolean isSync() {
        return sync;
    }

    public void setSync(boolean sync) {
        this.sync = sync;
    }
}

