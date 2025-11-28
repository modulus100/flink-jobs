package org.example.kafka.consumer;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.example.GreetingProtobuf;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class KafkaConsumerProtobufJob {

    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // ENV VARIABLES or defaults
        String bootstrap = getenvOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
        String topic     = getenvOrDefault("KAFKA_TOPIC", "demo-topic");
        String groupId   = getenvOrDefault("KAFKA_GROUP_ID", "flink-consumer-group");
        String schemaUrl = getenvOrDefault("SCHEMA_REGISTRY_URL", "http://localhost:8001/apis/ccompat/v7");

        // Kafka consumer core config
        Properties kafkaProps = new Properties();
        kafkaProps.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // <<< 🔥 ALL Schema Registry + OAuth config here —
        //     no more createSchemaRegistryClient(), Flink-safe >>>
        final Map<String, Object> confluent = new HashMap<>();
        confluent.put("schema.registry.url", schemaUrl);
        confluent.put("auto.register.schemas", false);
        confluent.put("value.subject.name.strategy",
                "io.confluent.kafka.serializers.subject.TopicRecordNameStrategy");

        // ========== OAUTH BEARER TOKEN AUTH FOR SCHEMA REGISTRY ==========
        confluent.put("bearer.auth.credentials.source", "OAUTHBEARER");
        confluent.put("bearer.auth.issuer.endpoint.url",
                getenvOrDefault("SR_OIDC_TOKEN_URL",
                        "http://localhost:8888/realms/apicurio/protocol/openid-connect/token"));
        confluent.put("bearer.auth.client.id",
                getenvOrDefault("SR_OIDC_CLIENT_ID", "apicurio-api"));
        confluent.put("bearer.auth.client.secret",
                getenvOrDefault("SR_OIDC_CLIENT_SECRET", "JeKMaLuvEwL5Tiyv9O5L6jJZfcsqiJYQ"));
        confluent.put("bearer.auth.scope", getenvOrDefault("SR_OIDC_SCOPE", ""));

        // 🔥 No SchemaRegistryClient passed. It will be created inside open().
        KafkaSource<GreetingProtobuf> source =
                KafkaSource.<GreetingProtobuf>builder()
                        .setBootstrapServers(bootstrap)
                        .setGroupId(groupId)
                        .setTopics(topic)
                        .setProperties(kafkaProps)
                        .setDeserializer(
                                new ConfluentProtobufKafkaRecordDeserializationSchema<>(
                                        GreetingProtobuf.class,
                                        confluent,
                                        schemaUrl
                                )
                        )
                        .build();

        // Print output
        env.fromSource(source, WatermarkStrategy.noWatermarks(), "protobuf-consumer")
                .map(GreetingProtobuf::toString)
                .print();

        env.execute("Kafka Protobuf Consumer with Schema Registry + OAuth");
    }

    private static String getenvOrDefault(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v;
    }
}
