# spring-kafka-perf

`spring-kafka-perf` is a small Spring Boot Kafka benchmark harness that can run in three startup modes:

- **Async mode**: producer-send performance using `KafkaTemplate`
- **Sync mode**: request/reply performance using `ReplyingKafkaTemplate`
- **Direct mode**: request/reply performance using raw `kafka-clients` `KafkaProducer` and `KafkaConsumer`

The project was created to compare Kafka performance between **Spring Kafka 3.x.x** and **Spring Kafka 4.x.x** based application setups.

The application runs one benchmark automatically at startup and logs a `PerfRunResult` with:

- requested count
- completed count
- timeout count
- total duration
- average round-trip / completion time
- max round-trip / completion time

## Switching between Spring Kafka 3.x.x and 4.x.x

Version switching is currently done manually in `pom.xml` by commenting and uncommenting the existing alternatives.

### `pom.xml` setup map

| Comparison target | `<parent>` version | Dependency block to keep active | Dependency block to keep commented |
| --- | --- | --- | --- |
| Spring Kafka 3.x.x setup | `3.5.11` | `Spring Boot 3` | `Spring Boot 4` |
| Spring Kafka 4.x.x setup | `4.0.3` | `Spring Boot 4` | `Spring Boot 3` |

### What is currently active

At the moment, `pom.xml` is set up for the Spring Boot 3 / Spring Kafka 3 line:

- the active parent version is `3.5.11`
- the `Spring Boot 3` dependency block is uncommented
- the `Spring Boot 4` dependency block is commented out

### How to switch to the Spring Kafka 4.x.x setup

Edit `pom.xml` and make these changes:

1. In the `<parent>` section:
   - comment out `<version>3.5.11</version>`
   - uncomment `<version>4.0.3</version>`
2. In the `<dependencies>` section:
   - uncomment the block labeled `Spring Boot 4`
   - comment out the block labeled `Spring Boot 3`

### How to switch back to the Spring Kafka 3.x.x setup

Reverse the same edits in `pom.xml`:

1. In the `<parent>` section:
   - uncomment `<version>3.5.11</version>`
   - comment out `<version>4.0.3</version>`
2. In the `<dependencies>` section:
   - uncomment the block labeled `Spring Boot 3`
   - comment out the block labeled `Spring Boot 4`

After changing the dependency set, rerun Maven so the new dependency graph is resolved.

```bat
cmd /c "mvn clean test"
```

### Quick verification after switching

Use this checklist against `pom.xml`:

- for the 3.x.x setup, `3.5.11` should be uncommented and the `Spring Boot 3` dependency block should be active
- for the 4.x.x setup, `4.0.3` should be uncommented and the `Spring Boot 4` dependency block should be active
- the opposite version and dependency block should remain commented out
- `mvn clean test` should resolve dependencies successfully after the edit

## Runtime modes

Use `kafka.perf.mode` to choose the startup benchmark path:

- `async`
- `sync`
- `direct`

### Async mode

- Enabled with `kafka.perf.mode=async`
- Uses `KafkaAsyncPerfService`
- Sends messages with `KafkaTemplate` and waits for the broker send result for each message
- Publishes to `kafka.perf.request-topic-async`

### Sync mode

- Enabled with `kafka.perf.mode=sync`
- Uses `KafkaSyncPerfService`
- Sends messages with `ReplyingKafkaTemplate`
- Waits for a reply on the configured reply topic
- The listener echoes the original payload back to the sender

### Direct mode

- Enabled with `kafka.perf.mode=direct`
- This is the current default in `src/main/resources/application.properties`
- Uses `KafkaClientsPerfService`
- Uses raw `KafkaProducer` / `KafkaConsumer` from `org.apache.kafka:kafka-clients`
- Starts an internal raw-client echo responder for the direct request topic
- Measures end-to-end request/reply without `KafkaTemplate`, `ReplyingKafkaTemplate`, or `@KafkaListener` in the benchmark path

## Configuration

The application properties currently defined in `src/main/resources/application.properties` are:

| Property | Default | Purpose |
| --- | --- | --- |
| `spring.kafka.bootstrap-servers` | `${KAFKA_BOOTSTRAP_SERVERS:localhost:29092}` | Kafka bootstrap servers |
| `kafka.perf.request-topic-sync` | `kafka-perf-request-topic-sync` | Request topic used by the sync path |
| `kafka.perf.request-topic-async` | `kafka-perf-request-topic-async` | Request topic configured for the async listener path |
| `kafka.perf.request-topic-client` | `kafka-perf-request-topic-client` | Request topic used by the direct `kafka-clients` path |
| `kafka.perf.reply-topic` | `kafka-perf-reply-topic` | Reply topic used by request/reply |
| `kafka.perf.reply-topic-client` | `kafka-perf-reply-topic-client` | Reply topic used by the direct `kafka-clients` path |
| `kafka.perf.consumer-group` | `kafka-perf-group` | Base consumer group for the listeners |
| `kafka.perf.message-count` | `10000` | Number of messages sent during the startup run |
| `kafka.perf.payload-bytes` | `256` | Minimum payload size for generated messages |
| `kafka.perf.timeout` | `30s` | Per-message timeout used by the benchmark |
| `kafka.perf.mode` | `direct` | Selects `async`, `sync`, or `direct` mode |

Additional wiring from the current code:

- `KafkaClientConfig` creates the sync, async, and direct request topics plus both reply topics automatically
- The reply listener container uses the group id `${kafka.perf.consumer-group}-reply`
- `RequestReplyPerfListener` listens on both request topics

## Local Kafka with Podman

This repository includes `docker-compose.yml` for a single-node Kafka broker. On Windows, the commands below use `cmd` so they are easy to run from PowerShell or the IDE terminal.

### Start Kafka

```bat
cmd /c "podman machine start"
cmd /c "podman compose -f docker-compose.yml up -d"
```

### Stop Kafka

```bat
cmd /c "podman compose -f docker-compose.yml down"
```

## Running the application

The project currently uses Maven directly; there is no Maven wrapper checked into the repository.

### Run with the current default configuration

```bat
cmd /c "mvn spring-boot:run"
```

This uses the current default from `src/main/resources/application.properties`, which is `kafka.perf.mode=direct`.

### Run in async mode

```bat
cmd /c "mvn spring-boot:run -Dspring-boot.run.arguments=--kafka.perf.mode=async"
```

### Run in sync mode

```bat
cmd /c "mvn spring-boot:run -Dspring-boot.run.arguments=--kafka.perf.mode=sync"
```

### Run in direct kafka-clients mode

```bat
cmd /c "mvn spring-boot:run -Dspring-boot.run.arguments=--kafka.perf.mode=direct"
```

### Override the message count for one run

```bat
cmd /c "mvn spring-boot:run -Dspring-boot.run.arguments=--kafka.perf.message-count=5000"
```

### Point the app at a different broker

```bat
cmd /c "set KAFKA_BOOTSTRAP_SERVERS=localhost:29092 && mvn spring-boot:run"
```

## What to expect in the logs

At startup, `KafkaPerfStartupRunner` chooses the mode from `kafka.perf.mode`, runs the benchmark once, and logs a line similar to:

- `Kafka perf run completed for mode direct: PerfRunResult(...)`

## Useful source files

- `src/main/java/org/example/springkafkaperf/perf/KafkaPerfStartupRunner.java`
- `src/main/java/org/example/springkafkaperf/perf/KafkaSyncPerfService.java`
- `src/main/java/org/example/springkafkaperf/perf/KafkaAsyncPerfService.java`
- `src/main/java/org/example/springkafkaperf/perf/KafkaClientsPerfService.java`
- `src/main/java/org/example/springkafkaperf/perf/RequestReplyPerfListener.java`
- `src/main/java/org/example/springkafkaperf/config/KafkaClientConfig.java`
- `src/main/resources/application.properties`

## Build / test

```bat
cmd /c "mvn test"
```

