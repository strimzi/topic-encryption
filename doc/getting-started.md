# Getting started

Requirements:
- a Kafka instance, version 2.8.0 or older, which you can configure
- Java 17
- Apache maven installed in your command line environment
- git command


The steps for getting started with this initial version of topic encryption are outlined below:
1. Clone the repository and set your working path.
2. Compile
3. Configure the Kafka advertised address
4. Configure the proxy
5. Run the proxy
6. Start kafka
7. Run kafka clients

Each of these steps is described in detail below with an example.

## Scenario

In the scenario to get started, all components run on the same system, `localhost`.  The Kafka broker can also run remotely. The minimum requirement is that one can update the broker configuration file and restart the broker. In this example, however, we run the broker locally.

The proxy will listen on port 1234 and the broker listens on its standard port 9092 as depicted below:

```
 Kafka client        Proxy         Kafka broker
      o------------o 1234 o------------o 9092
```

The clients are reconfigured to use port 1234 (details below).

A policy to encrypt all topics with the same key, along with a test key management system (KMS) which returns a hard-coded AES key, is used.

The following sections provide details for each step in running the encrypting proxy.

### 1. Clone the repository and set your working path
```
git clone git@github.com:strimzi/topic-encryption.git
cd topic-encryption
```

### 2. Compile

```
mvn install
```

### 3. Configure the Kafka broker's listeners
The address advertised by Kafka must be that of the proxy, not the broker itself.

Modify the `advertised.listeners` property in `$KAFKA_HOME/config/server.properties` to point to the proxy host and port, as shown in the snippet below:

```
# The address the socket server listens on. It will get the value returned from 
# java.net.InetAddress.getCanonicalHostName() if not configured.
#   FORMAT:
#     listeners = listener_name://host_name:port
#   EXAMPLE:
#     listeners = PLAINTEXT://your.host.name:9092
listeners=PLAINTEXT://:9092

# Hostname and port the broker will advertise to producers and consumers. If not set, 
# it uses the value for "listeners" if configured.  Otherwise, it will use the value
# returned from java.net.InetAddress.getCanonicalHostName().
advertised.listeners=PLAINTEXT://127.0.0.1:1234
```
Stop the Kafka broker and start it after the proxy is running.

### 4. Configure the proxy
Set the working directory to the proxy's target folder:
```
$ cd vertx-proxy/target/
```

Create a configuration file, `config.json` and add the following JSON contents:

```
{
  "listening_port" : 1234,
  "kafka_broker"   : "localhost:9092",
  "policy_repo"    : "test"
}
```
### 5. Run the proxy
With the current path set to the target directory, run the proxy with the following Java invocation:

```
$ java -jar vertx-proxy-0.0.1-SNAPSHOT-fat.jar
```

If successfully started, the following output appears:
```
$ java -jar vertx-proxy-0.0.1-SNAPSHOT-fat.jar
WARNING: sun.reflect.Reflection.getCallerClass is not supported. This will impact performance.
2022-04-13 10:30:12 INFO  KafkaProxyVerticle:46 35 - Kafka version: 2.8.0
2022-04-13 10:30:12 INFO  KafkaProxyVerticle:75 35 - Listening on port 1234
```

### 6. Start Kafka broker

Now start the Kafka broker, for example:
```
$KAFKA_HOME/bin/kafka-server-start.sh config/server.properties 
```

### 7. Run Kafka clients
Start the Kafka console producer (note the proxy address in the broker list):

```
$KAFKA_HOME/bin/kafka-console-producer.sh  --broker-list localhost:1234 --topic enctest --producer.config config/producer.properties 
```

Start the Kafka console consumer, like the producer, specifying the proxy host and port:
```
$KAFKA_HOME/bin/kafka-console-consumer.sh --bootstrap-server localhost:1234 --consumer.config config/consumer.properties --topic enctest  --from-beginning
```

Enter arbitry data in the producer and verify that it appears in consumer. 

Inspect the topic segment files and verify they indeed are encrypted.
```
$KAFKA_HOME/kafka-dump-log.sh --files /tmp/kafka-logs/enctest-0/00000000000000000000.log --value-decoder-class kafka.serializer.StringDecoder
```
