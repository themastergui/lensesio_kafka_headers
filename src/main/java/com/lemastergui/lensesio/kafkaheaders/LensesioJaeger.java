package com.lemastergui.lensesio.kafkaheaders;


import com.google.common.collect.ImmutableMap;
import io.jaegertracing.Configuration;
import io.jaegertracing.internal.JaegerTracer;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.contrib.kafka.TracingKafkaProducer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.Future;

public class LensesioJaeger {

    private final Tracer tracer;

    private LensesioJaeger(Tracer tracer) {
        this.tracer = tracer;
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("Expecting two arguments: Kafka Broker (eg: mybroker.com:9092) & message string");
        }

        String broker = args[0];
        String message = args[1];
        //JaegerTracer tracer = getTracer("lenses.io_test");
        try (JaegerTracer tracer = initTracer("lenses.io_test")) {
            new LensesioJaeger(tracer).sendMessage(broker, message);
        }
    }

    public static JaegerTracer initTracer(String service) {
        Configuration.SamplerConfiguration samplerConfig = Configuration.SamplerConfiguration.fromEnv().withType("const").withParam(1);
        Configuration.ReporterConfiguration reporterConfig = Configuration.ReporterConfiguration.fromEnv().withLogSpans(true);
        Configuration config = new Configuration(service).withSampler(samplerConfig).withReporter(reporterConfig);
        return config.getTracer();
    }

    private static KafkaProducer<String, String> createProducer(String broker) {

        final Properties kafkaProperties = new Properties();
        kafkaProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, broker);
        kafkaProperties.put("enable.auto.commit", "false");
        kafkaProperties.put("group.id", "payments_processing");
        kafkaProperties.put("key.serializer", StringSerializer.class.getName());
        kafkaProperties.put("value.serializer", StringSerializer.class.getName());
        return new KafkaProducer<>(kafkaProperties);
    }

    public void sendMessage(String broker, String message) {
        try {
            KafkaProducer<String, String> kafkaProducer = createProducer(broker);
            TracingKafkaProducer producer = new TracingKafkaProducer(kafkaProducer, tracer);
            Span span = tracer.buildSpan("sendMessage").start();
            try (Scope scope = tracer.scopeManager().activate(span)) {
                ProducerRecord pr = new ProducerRecord<>("traces", null, "111", message);
                final Future<RecordMetadata> future = producer.send(pr);
                producer.flush();
                span.log(ImmutableMap.of("event", "sendMessage", "value", message));
            } finally {
                span.finish();
            }
        } catch (Exception e) {
            System.out.println("An error occurred while sending message to Kafka. " + e.getMessage());
            e.printStackTrace();
        }
    }
}
