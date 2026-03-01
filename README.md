# java-sapient-test-harness

Spring Boot test harness for [java-sapient-sdk](https://github.com/cafanasyev/java-sapient-sdk) — a Java SDK for [BSI Flex 335 v2.0](https://www.bsigroup.com/en-US/insights-and-media/insights/brochures/bsi-flex-335-interface-of-the-sapient-sensor-management-specification/) SAPIENT. Exercises and demonstrates the SDK via REST endpoints backed by simulated edge nodes loaded from local JSON fixtures.

## Requirements

- Java 21+
- No local Maven installation required (Maven Wrapper included)
- `java-sapient-sdk:0.1.0` must be installed in the local Maven repository (`~/.m2`) before building

## Build

```bash
./mvnw compile
```

## Run

```bash
./mvnw spring-boot:run
```

## Test

```bash
# Unit tests
./mvnw test

# Unit + static analysis
./mvnw verify
```

## Structure

| Package | Description |
|---|---|
| `io.sapient` | Application entry point and SDK wiring (`SapientConfig`) |
| `io.sapient.test.harness` | Edge node loading, registry, and REST endpoints |

## REST API

| Method | Path | Description |
|---|---|---|
| `GET` | `/nodes` | List all loaded nodes with registration and status report |
| `POST` | `/nodes/reload` | Reload nodes from the `edge_nodes/` directory |
| `PUT` | `/nodes/{id}/online` | Override the online status of a node |

Swagger UI is available at `http://localhost:8080/swagger-ui.html` when the application is running.

## Edge node fixtures

Place UUID-named subdirectories under `edge_nodes/` (relative to the working directory), each containing:

```
edge_nodes/
  {uuid}/
    registration.json
    status_report.json
```

JSON files must match the BSI Flex 335 v2.0 Protobuf schema.

## Code Quality

The project uses three static analysis tools that run automatically during the build:

| Tool | What it does | Runs during |
|---|---|---|
| [Spotless](https://github.com/diffplug/spotless) | Code formatting via [google-java-format](https://github.com/google/google-java-format) (AOSP style, 4-space indentation) | `validate` phase |
| [Error Prone](https://errorprone.info/) | Compile-time bug detection by Google | `compile` phase |
| [SpotBugs](https://spotbugs.github.io/) | Bytecode-level bug detection | `verify` phase |

### Standalone commands

```bash
# Check formatting
./mvnw spotless:check

# Auto-fix formatting
./mvnw spotless:apply

# Run SpotBugs analysis
./mvnw spotbugs:check

# Open SpotBugs GUI report
./mvnw spotbugs:gui
```

### IDE setup

Install the [google-java-format](https://plugins.jetbrains.com/plugin/8527-google-java-format) IntelliJ IDEA plugin and select **AOSP** style in its settings. This makes the IDE formatter produce identical output to Spotless.

## License

This project is released into the public domain under the [Unlicense](https://unlicense.org).
