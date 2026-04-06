# DeltaSpike Transactional Config Add-on

A CDI extension that provides transactional configuration snapshots using
[Apache DeltaSpike](https://deltaspike.apache.org/)'s partial-bean and
config-source mechanisms.

## Overview

This add-on allows you to define type-safe configuration interfaces that
automatically take a consistent snapshot of all configuration values for
the duration of a transaction. Within a transaction, repeated reads of
the same config key always return the same value, even if the underlying
source changes.

### Features

- **Type-safe config interfaces** via DeltaSpike's `@PartialBeanBinding`
- **Transactional snapshots** using thread-local immutable maps
- **Try-with-resources** support via `AutoCloseable`
- **Refresh support** via `RefreshAware` interface
- **Snapshot-aware sources** with change detection and automatic retry
- **Pluggable value converters** via `ConverterFactory` SPI

## Usage

Define a configuration interface:

```java
@TransactionalConfig
public interface AppConfig extends ConfigTransactionAware {
    String databaseUrl();
    Integer maxConnections();
}
```

Inject and use it — values are automatically snapshotted per request:

```java
@Inject
private AppConfig config;

public void doWork() {
    // Within a request, these always return the same values
    String url = config.databaseUrl();
    int max = config.maxConnections();
}
```

Or use explicit transaction scoping:

```java
try (AppConfig txConfig = config) {
    // Snapshot is frozen here
    String url = txConfig.databaseUrl();
}
// Snapshot is released
```

## Requirements

- Java 25+
- Maven 3.6.3+
- Jakarta CDI 4.1
- DeltaSpike 2.0.0 (core + partial-bean)

## Building

```bash
mvn clean verify
```

## Testing

Tests use the [dynamic-cdi-test-bean-addon](https://github.com/os890/dynamic-cdi-test-bean-addon)
with `@EnableTestBeans` for CDI SE integration testing.

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.
