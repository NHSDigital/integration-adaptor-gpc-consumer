# integration-adaptor-gpc-consumer

National Integration Adaptor - GP Connect Consumer

## Requirements:

1. JDK 11

## Configuration

The adaptor reads its configuration from environment variables. The following sections describe the environment variables
 used to configure the adaptor.

Variables without a default value and not marked optional, *MUST* be defined for the adaptor to run.

### General Configuration Options

| Environment Variable                 | Default                   | Description
| -------------------------------------|---------------------------|-------------
| GPC_CONSUMER_SERVER_PORT             | 8080                      | The port on which the GPC Consumer Adapter will run.
| GPC_CONSUMER_ROOT_LOGGING_LEVEL      | WARN                      | The logging level applied to the entire application (including third-party dependencies).
| GPC_CONSUMER__LOGGING_LEVEL          | INFO                      | The logging level applied to GPC Consumer adaptor components.
| GPC_CONSUMER__LOGGING_FORMAT         | (*)                       | Defines how to format log events on stdout

Logging levels are ane of: DEBUG, INFO, WARN, ERROR

The level DEBUG **MUST NOT** be used when handling live patient data.

(*) GPC Consumer adaptor uses logback (http://logback.qos.ch/). The built-in [logback.xml](service/src/main/resources/logback.xml) 
defines the default log format. This value can be overridden using the `GPC_CONSUMER_LOGGING_FORMAT` environment variable.
You can provide an external `logback.xml` file using the `-Dlogback.configurationFile` JVM parameter.

## How to run service:

TODO

## How to run tests

**Warning**: Gradle uses a [Build Cache](https://docs.gradle.org/current/userguide/build_cache.html) to re-use compile and
test outputs for faster builds. To re-run passing tests without making any code changes you must first run 
`./gradlew clean` to clear the build cache. Otherwise, gradle uses the cached outputs from a previous test execution to 
pass the build.

You must run all gradle commands from the `service/` directory.

### How to run unit tests:

```shell script
./gradlew test
```

### How to run all checks:

```shell script
./gradlew check
```

### How to run integration tests:

```shell script
./gradlew integrationTest
```

## Licensing
This code is dual licensed under the MIT license and the OGL (Open Government License). Any new work added to this repository must conform to the conditions of these licenses. In particular this means that this project may not depend on GPL-licensed or AGPL-licensed libraries, as these would violate the terms of those libraries' licenses.

The contents of this repository are protected by Crown Copyright (C).
