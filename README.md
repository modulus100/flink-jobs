# Flink SQL: Kafka → Kafka with Confluent Schema Registry (Protobuf)

This repo contains both a DataStream example and a pure Flink SQL example that reads from a Kafka topic with Protobuf data (Schema Registry) and writes to another Kafka topic.

## Components overview
- **JobManager**: Coordinates Flink jobs, scheduling, checkpoints, high availability.
- **TaskManager**: Executes the tasks (operators) of the jobs. You usually run one or more TMs.
- **SQL Client**: CLI tool to execute Flink SQL statements/scripts. Can run embedded or submit to a cluster.
- **SQL Gateway (optional)**: REST service to submit/manage SQL statements programmatically.

## Prerequisites
- Java 21 toolchain (configured via Gradle toolchain).
- Docker and Docker Compose.
- Flink distribution if running the SQL Client on the host (optional for Docker-only flow).

## Prepare dependencies (Confluent Protobuf format + Kafka connector)
- Build and gather Flink SQL runtime jars:

```sh
./gradlew :kafka-sql-protobuf:prepareSqlLibs
```

This produces `kafka-sql-protobuf/build/sql-libs/` with the Kafka connector and `io.confluent.flink:flink-sql-protobuf` so the `format = 'protobuf-confluent'` works.

## Bring up Kafka + Schema Registry (Apicurio)
Use the main compose file to start infra:

```sh
docker compose up -d kafka postgres keycloak registry registry-ui kafdrop
```

Using Podman instead of Docker:
```sh
podman compose up -d kafka postgres keycloak registry registry-ui kafdrop
```

- Kafka bootstrap (host): `localhost:9092`
- Schema Registry (host): `http://localhost:8001/apis/ccompat/v7`

## Run Flink SQL (two options)

### Option A: Host SQL Client (quickest)
- Use your local Flink 1.20 SQL Client:

```sh
$FLINK_HOME/bin/sql-client.sh \
  -l kafka-sql-protobuf/build/sql-libs \
  -f kafka-sql-protobuf/sql/kafka_to_kafka.sql
```

- Keep host endpoints in the SQL file:
  - `'properties.bootstrap.servers' = 'localhost:9092'`
  - `'schema.registry.url' = 'http://localhost:8001/apis/ccompat/v7'`

The client runs an embedded mini-cluster and stops the job when the client exits.

### Option B: Run a Flink cluster in a separate compose and submit SQL
A dedicated compose file is provided: `docker-compose.flink.yml`.

0) Ensure connector/format jars are available to the cluster
- The compose file mounts `kafka-sql-protobuf/build/sql-libs` into `/opt/flink/lib` on JobManager and TaskManager.
- Build them first:
```sh
./gradlew :kafka-sql-protobuf:prepareSqlLibs
```
- Alternative (if you don’t mount libs): start SQL Client and load jars manually:
```sql
ADD JAR '/opt/sql-libs/<jar-name>.jar';
```

1) Create a shared network once:

```sh
docker network create flink-net
```

Podman:
```sh
podman network create flink-net
```

2) Connect existing services to the network:

```sh
docker network connect flink-net kafka
docker network connect flink-net apicurio-registry
```

Podman:
```sh
podman network connect flink-net kafka
podman network connect flink-net apicurio-registry
```

3) Start Flink cluster:

```sh
docker compose -f docker-compose.flink.yml up -d jobmanager taskmanager
```

Podman:
```sh
podman compose -f docker-compose.flink.yml up -d jobmanager taskmanager
```

4) Submit SQL using the containerized SQL Client:

```sh
# Ensure the SQL file uses in-network endpoints:
#   'properties.bootstrap.servers' = 'kafka:29092'
#   'schema.registry.url' = 'http://registry:8001/apis/ccompat/v7'

docker compose -f docker-compose.flink.yml run --rm sql-client
```

Podman:
```sh
podman compose -f docker-compose.flink.yml run --rm sql-client
```

Alternatively, start the SQL Gateway for REST submissions:

```sh
docker compose -f docker-compose.flink.yml up -d sql-gateway
# REST API now at http://localhost:8083
```

Podman:
```sh
podman compose -f docker-compose.flink.yml up -d sql-gateway
# REST API now at http://localhost:8083
```

## SQL script location
- `kafka-sql-protobuf/sql/kafka_to_kafka.sql`
  - Defines Kafka `src` and `sink` tables with `format = 'protobuf-confluent'`.
  - Performs `INSERT INTO sink SELECT * FROM src`.

## DataStream example (unrelated to SQL)
- `kafka-consumer-protobuf` shows a Kafka consumer using Confluent Protobuf deserializer and Schema Registry with OAuth.

## OAuth note (DataStream path)
Flink requires whitelisting of the OAuth2 token URL when using Schema Registry over HTTP. Run JVM with:

```sh
-Dorg.apache.kafka.sasl.oauthbearer.allowed.urls=http://localhost:8888/realms/apicurio/protocol/openid-connect/token
