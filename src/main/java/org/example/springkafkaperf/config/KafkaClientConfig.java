package org.example.springkafkaperf.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;

@Configuration
@EnableKafka
public class KafkaClientConfig {

    @Bean
    public NewTopic kafkaPerfRequestTopicSync(KafkaPerfProperties properties) {
        return TopicBuilder.name(properties.getRequestTopicSync())
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic kafkaPerfReplyTopic(KafkaPerfProperties properties) {
        return TopicBuilder.name(properties.getReplyTopic())
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic kafkaPerfRequestTopicAsync(KafkaPerfProperties properties) {
        return TopicBuilder.name(properties.getRequestTopicAsync())
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> perfKafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            KafkaTemplate<String, String> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setReplyTemplate(kafkaTemplate);
        factory.getContainerProperties().setPollTimeout(1_000L);
        return factory;
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, String> replyContainer(
            ConsumerFactory<String, String> consumerFactory,
            KafkaPerfProperties perfProperties) {
        ContainerProperties containerProperties = new ContainerProperties(perfProperties.getReplyTopic());
        containerProperties.setGroupId(perfProperties.getConsumerGroup() + "-reply");
        return new ConcurrentMessageListenerContainer<>(consumerFactory, containerProperties);
    }

    @Bean
    public ReplyingKafkaTemplate<String, String, String> replyKafkaTemplate(
            ProducerFactory<String, String> producerFactory,
            ConcurrentMessageListenerContainer<String, String> replyContainer,
            KafkaPerfProperties perfProperties) {
        ReplyingKafkaTemplate<String, String, String> template =
                new ReplyingKafkaTemplate<>(producerFactory, replyContainer);
        template.setSharedReplyTopic(true);
        template.setDefaultReplyTimeout(perfProperties.getTimeout());
        return template;
    }
}

