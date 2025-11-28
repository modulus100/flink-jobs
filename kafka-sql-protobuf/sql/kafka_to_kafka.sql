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
  'schema.registry.url' = 'http://localhost:8001/apis/ccompat/v7',
  -- OAuth for Schema Registry (matches KafkaConsumerProtobufJob)
  'bearer.auth.credentials.source' = 'OAUTHBEARER',
  'bearer.auth.issuer.endpoint.url' = 'http://localhost:8888/realms/apicurio/protocol/openid-connect/token',
  'bearer.auth.client.id' = 'apicurio-api',
  'bearer.auth.client.secret' = 'JeKMaLuvEwL5Tiyv9O5L6jJZfcsqiJYQ',
  'bearer.auth.scope' = '',
  -- Subject strategy + registration behavior
  'auto.register.schemas' = 'false',
  'value.subject.name.strategy' = 'io.confluent.kafka.serializers.subject.TopicRecordNameStrategy'
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
  -- OAuth for Schema Registry (matches KafkaConsumerProtobufJob)
  'bearer.auth.credentials.source' = 'OAUTHBEARER',
  'bearer.auth.issuer.endpoint.url' = 'http://localhost:8888/realms/apicurio/protocol/openid-connect/token',
  'bearer.auth.client.id' = 'apicurio-api',
  'bearer.auth.client.secret' = 'JeKMaLuvEwL5Tiyv9O5L6jJZfcsqiJYQ',
  'bearer.auth.scope' = '',
  -- Subject strategy + registration behavior
  'auto.register.schemas' = 'false',
  'value.subject.name.strategy' = 'io.confluent.kafka.serializers.subject.TopicRecordNameStrategy',
  'sink.partitioner' = 'fixed'
);

INSERT INTO sink SELECT * FROM src;
