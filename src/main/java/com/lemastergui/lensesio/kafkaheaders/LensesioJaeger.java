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
        if (args.length != 1) {
            throw new IllegalArgumentException("Expecting one argument");
        }

        String message = args[0];
        //JaegerTracer tracer = getTracer("lenses.io_test");
        try (JaegerTracer tracer = initTracer("lenses.io_test")) {
            new LensesioJaeger(tracer).sendMessage(message);
        }
    }

    public static JaegerTracer initTracer(String service) {
        Configuration.SamplerConfiguration samplerConfig = Configuration.SamplerConfiguration.fromEnv().withType("const").withParam(1);
        Configuration.ReporterConfiguration reporterConfig = Configuration.ReporterConfiguration.fromEnv().withLogSpans(true);
        Configuration config = new Configuration(service).withSampler(samplerConfig).withReporter(reporterConfig);
        return config.getTracer();
    }

    private static KafkaProducer<String, String> createProducer() {

        final Properties kafkaProperties = new Properties();
        kafkaProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "35.180.136.176:9092");
        kafkaProperties.put("enable.auto.commit", "false");
        kafkaProperties.put("group.id", "payments_processing");
        kafkaProperties.put("key.serializer", StringSerializer.class.getName());
        kafkaProperties.put("value.serializer", StringSerializer.class.getName());
        return new KafkaProducer<>(kafkaProperties);
    }

    public void sendMessage(String message) {
        try {
            KafkaProducer<String, String> kafkaProducer = createProducer();
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
