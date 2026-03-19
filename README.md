# spring-kafka-perf

This project includes a synchronous Kafka request/reply performance harness.

## What was added

- Request/reply service based on `ReplyingKafkaTemplate`
- Request listener using `@KafkaListener` + `@SendTo`
- Sync run metrics: requested, replied, timeout, total duration, average/max round-trip
- Optional startup runner for one sync performance run

## Configuration

Key properties in `src/main/resources/application.properties`:

- `kafka.perf.request-topic`
- `kafka.perf.reply-topic`
- `kafka.perf.consumer-group`
- `kafka.perf.message-count`
- `kafka.perf.payload-bytes`
- `kafka.perf.timeout`

Kafka bootstrap server defaults to `localhost:29092` and can be overridden by `KAFKA_BOOTSTRAP_SERVERS`.

## Podman prerequisites

```bat
cmd /c "podman machine start"
cmd /c "podman system connection default"
```

## Start the app

```bat
cmd /c "mvnw.cmd spring-boot:run"
```

On startup, the app runs synchronous request/reply messages and logs `SyncPerfRunResult`.

## Trigger a custom run size at startup

```bat
cmd /c "mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--kafka.perf.message-count=5000"
```

## Run tests

```bat
cmd /c "mvnw.cmd test"
```

