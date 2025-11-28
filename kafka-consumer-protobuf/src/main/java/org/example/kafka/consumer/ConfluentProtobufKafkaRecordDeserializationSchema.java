package org.example.kafka.consumer;

import com.google.protobuf.Message;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchemaProvider;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ConfluentProtobufKafkaRecordDeserializationSchema<T extends Message>
        implements KafkaRecordDeserializationSchema<T> {

    private final Class<T> targetType;
    private final Map<String, ?> config;
    private final String schemaRegistryUrl;

    private transient KafkaProtobufDeserializer<T> deserializer;

    public ConfluentProtobufKafkaRecordDeserializationSchema(
            Class<T> targetType,
            Map<String, ?> config,
            String schemaRegistryUrl) {

        this.targetType = Objects.requireNonNull(targetType);
        this.config = config != null ? config : Map.of();
        this.schemaRegistryUrl = Objects.requireNonNull(schemaRegistryUrl);
    }

    @Override
    public void open(DeserializationSchema.InitializationContext context) {
        Map<String, Object> effective = new HashMap<>(config);
        effective.putIfAbsent("specific.protobuf.value.type", targetType.getName());
        effective.putIfAbsent("schema.registry.url", schemaRegistryUrl);

        var schemaRegistryClient = new CachedSchemaRegistryClient(
                List.of(schemaRegistryUrl),
                1000,
                List.of(new ProtobufSchemaProvider()),
                effective);
        this.deserializer = new KafkaProtobufDeserializer<>(schemaRegistryClient);
        this.deserializer.configure(effective, false);
    }

    @Override
    public void deserialize(ConsumerRecord<byte[], byte[]> record, Collector<T> out) {
        T msg = deserializer.deserialize(record.topic(), record.value());
        if (msg != null) out.collect(msg);
    }

    @Override
    public TypeInformation<T> getProducedType() {
        return TypeInformation.of(targetType);
    }
}
