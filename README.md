

# OpenTracing Kafka Producer Client Test

Very basic Kafka Producer using OpenTelemetry Kafka Client to send a test message to a traces topic in Kafka.

## Run single-broker Kafka docker

For a Kafka environment, use Lenses Box docker. Get a free license key from: [https://lenses.io/box](https://lenses.io/box)

```bash
docker run -e ADV_HOST=YOURHOST -e EULA="<<LICENSE KEY>>" --rm -p 3030:3030 -p 9092:9092 lensesio/box
```

## Run Jaeger

Run the Jaeger client with:

```bash
docker run -d --name jaeger   -e COLLECTOR_ZIPKIN_HTTP_PORT=9411   -p 5775:5775/udp   -p 6831:6831/udp   -p 6832:6832/udp   -p 5778:5778   -p 16686:16686   -p 14268:14268   -p 14250:14250   -p 9411:9411   jaegertracing/all-in-one:latest
```

Run the application sending a two arguments: a broker hostname:port & the test message to publish to the Kafka topic. Eg:
```bash
com.lemastergui.lensesio.kafkaheaders.LensesioJaeger 35.181.136.177:9092 hello
```

## Veiw Trace IDs with Lenses

Use lenses.io to view trace IDs in Kafka Message Headers.

Example Lenses Snapshot SQL command:
```sql
SELECT _value as message, TAKELEFT(HEADERASSTRING("uber-trace-id"),INDEXOF(HEADERASSTRING("uber-trace-id"),":")) as trace_id FROM traces 
  LIMIT 100;
```#
