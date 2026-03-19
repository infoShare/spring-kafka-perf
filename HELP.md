# spring-kafka-perf help

This file is a short project reference. For the full quick-start and run examples, see `README.md`.

## Project goal

This repository was created to compare Kafka performance across two dependency lines:

- Spring Kafka 3.x.x
- Spring Kafka 4.x.x

The switching mechanism is currently a manual `pom.xml` edit.

## Switching versions in `pom.xml`

### Setup map

| Target setup | `<parent>` version | Active dependency block |
| --- | --- | --- |
| Spring Kafka 3.x.x | `3.5.11` | `Spring Boot 3` |
| Spring Kafka 4.x.x | `4.0.3` | `Spring Boot 4` |

The current `pom.xml` is set to the Spring Boot 3 / Spring Kafka 3 path:

- `<version>3.5.11</version>` is active in the `<parent>` section
- `<version>4.0.3</version>` is present but commented out
- the dependency block labeled `Spring Boot 3` is active
- the dependency block labeled `Spring Boot 4` is commented out

To switch to the Spring Kafka 4.x.x setup:

1. In the `<parent>` section, comment out `3.5.11` and uncomment `4.0.3`
2. In `<dependencies>`, uncomment the `Spring Boot 4` block
3. In `<dependencies>`, comment out the `Spring Boot 3` block

To switch back to the Spring Kafka 3.x.x setup, reverse those edits.

After changing `pom.xml`, refresh dependencies by running:

```bat
cmd /c "mvn clean test"
```

Quick verification:

- only one parent version should be uncommented
- only one of the `Spring Boot 3` / `Spring Boot 4` dependency blocks should be active
- the active parent version and active dependency block should describe the same target setup

## Current startup behavior

- The application executes one benchmark at startup through `KafkaPerfStartupRunner`
- Mode selection is controlled by `kafka.perf.sync`
- `true` runs the synchronous request/reply path
- `false` runs the asynchronous producer-send path

## Key properties

These are the project-specific properties currently bound by `KafkaPerfProperties`:

- `kafka.perf.request-topic-sync`
- `kafka.perf.request-topic-async`
- `kafka.perf.reply-topic`
- `kafka.perf.consumer-group`
- `kafka.perf.message-count`
- `kafka.perf.payload-bytes`
- `kafka.perf.timeout`
- `kafka.perf.sync`

The Kafka broker address is configured through:

- `spring.kafka.bootstrap-servers`
- `KAFKA_BOOTSTRAP_SERVERS` environment variable override

## Sync vs async at a glance

### Sync mode

- Service: `KafkaSyncPerfService`
- Template: `ReplyingKafkaTemplate<String, String, String>`
- Request topic: `kafka.perf.request-topic-sync`
- Reply topic: `kafka.perf.reply-topic`
- Listener method: `RequestReplyPerfListener.onRequestSync`

### Async mode

- Service: `KafkaAsyncPerfService`
- Template: `KafkaTemplate<String, String>`
- Listener topic property: `kafka.perf.request-topic-async`
- Listener method: `RequestReplyPerfListener.onRequestAsync`

## Topic and listener wiring

`KafkaClientConfig` creates three topics:

- sync request topic
- async request topic
- reply topic

The reply listener container uses:

- topic: `kafka.perf.reply-topic`
- group id: `${kafka.perf.consumer-group}-reply`

## Local run reminders

- Use `podman compose -f docker-compose.yml up -d` to start Kafka locally
- Use `mvn spring-boot:run` to start the application
- Use `-Dspring-boot.run.arguments=--kafka.perf.sync=true` to switch to sync mode for one run

## Useful references

- [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
- [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/4.0.3/maven-plugin)
- [Spring for Apache Kafka](https://docs.spring.io/spring-boot/4.0.3/reference/messaging/kafka.html)

