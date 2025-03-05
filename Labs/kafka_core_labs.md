# Kafka Multi Broker Setup

**Kafka local setup:**
* docker compose -f .\docker-compose-kafka-cluster.yaml up -d

**Kafka** commands:
* **[List all topics]** `kafka-topics --bootstrap-server kafka-broker-1:19092,kafka-broker-2:19093,kafka-broker-3:19094 --list`
* **[Create topic]** `kafka-topics --bootstrap-server kafka-broker-1:19092,kafka-broker-2:19093,kafka-broker-3:19094 --create --topic test-topic --replication-factor 3 --partitions 3`
* **[Describe topic]** `kafka-topics --bootstrap-server kafka-broker-1:19092,kafka-broker-2:19093,kafka-broker-3:19094 --describe --topic test-topic`

* **Kafka console producer** command to produce messages **without key**:
  `kafka-console-producer --bootstrap-server kafka-broker-1:19092,kafka-broker-2:19093,kafka-broker-3:19094 --topic test-topic`

* **Kafka console consumer** command to consume messages **without key**:
  `kafka-console-consumer --bootstrap-server kafka-broker-1:19092,kafka-broker-2:19093,kafka-broker-3:19094 --topic test-topic`

* **Kafka console producer** command to produce messages **with key**:
  `kafka-console-producer --bootstrap-server kafka-broker-1:19092,kafka-broker-2:19093,kafka-broker-3:19094 --topic test-topic --property "key.separator=-" --property "parse.key=true"`

* **Kafka console consumer** command to consume messages **with key**:
  `kafka-console-consumer --bootstrap-server kafka-broker-1:19092,kafka-broker-2:19093,kafka-broker-3:19094 --topic test-topic --property "key.separator=-" --property "print.key=true"`
* **Kafka console consumer** command to consume messages **from beginning**:
  `kafka-console-consumer --bootstrap-server kafka-broker-1:19092,kafka-broker-2:19093,kafka-broker-3:19094 --topic test-topic --from-beginning`

* **Kafka console consumer** command to consume messages **with key and consumer groups**:
  `kafka-console-consumer --bootstrap-server kafka-broker-1:19092,kafka-broker-2:19093,kafka-broker-3:19094 --topic test-topic --property "key.separator=-" --property "print.key=true" --group group1`

**Kafka Commit Log & Retention Period**:
* Server.properties location: /etc/kafka/server.properties
* Property name for commit log dir: **log.dirs**
* Property name for Retention period: **log.retention.hours**

**Kafka Configs command**
* **Set min.insync.replicas property**
  `kafka-configs --bootstrap-server kafka-broker-1:19092,kafka-broker-2:19093,kafka-broker-3:19094 --entity-type topics --entity-name test-topic --alter --add-config min.insync.replicas=2`