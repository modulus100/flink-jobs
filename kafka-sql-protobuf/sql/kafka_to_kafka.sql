-- Flink SQL: Kafka -> Kafka using Confluent Schema Registry (Protobuf)

-- Adjust these properties in the WITH clauses below as needed:
--  - properties.bootstrap.servers
--  - schema.registry.url
--  - topic names

CREATE TABLE src (
  message STRING,
  message2 STRING,
  `timestamp` BIGINT,
  sender ROW<name STRING, email STRING>,
  metadata ROW<language STRING, region STRING, tags MAP<STRING, STRING>>,
  event ROW<id STRING, source STRING, type STRING>
) WITH (
  'connector' = 'kafka',
  'topic' = 'protobuf-input',
  'properties.bootstrap.servers' = 'localhost:9092',
  'properties.group.id' = 'flink-sql-group',
  'scan.startup.mode' = 'earliest-offset',
  'format' = 'protobuf-confluent',
  'schema.registry.url' = 'http://localhost:8001/apis/ccompat/v7'
);

CREATE TABLE sink (
  message STRING,
  message2 STRING,
  `timestamp` BIGINT,
  sender ROW<name STRING, email STRING>,
  metadata ROW<language STRING, region STRING, tags MAP<STRING, STRING>>,
  event ROW<id STRING, source STRING, type STRING>
) WITH (
  'connector' = 'kafka',
  'topic' = 'protobuf-output',
  'properties.bootstrap.servers' = 'localhost:9092',
  'format' = 'protobuf-confluent',
  'schema.registry.url' = 'http://localhost:8001/apis/ccompat/v7',
  'sink.partitioner' = 'fixed'
);

INSERT INTO sink SELECT * FROM src;
