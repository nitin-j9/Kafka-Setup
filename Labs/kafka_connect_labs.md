# Kafka Connector Setup

**Kafka Connector local setup:**
* **Infra setup** docker compose -f .\docker-compose-connect.yaml up -d
* **Database setup**
* exec in postgres container
  * Login to Psql `psql --host=127.0.0.1 --port=5432 --dbname=product_db --username=root`
* Execute below queries  
  * `CREATE SCHEMA CATALOG;`
  * `CREATE TABLE IF NOT EXISTS catalog.outbox_event (      
    id uuid NOT NULL,      
    "data" jsonb NULL,      
    "aggregate" varchar(255) NULL,      
    aggregate_id varchar(255) NULL,      
    "source" varchar(255) NULL,      
    "type" varchar(255) NULL,      
    schema_version varchar(255) NULL,      
    trace_id varchar(255) NULL,      
    "time" timestamp NULL,      
    CONSTRAINT outbox_event_pkey PRIMARY KEY (id)      
    );`
* **Create Kafka Connector**
  * exec kafka-connect container
  * Execute  [REST API](../connectors-guide/connector.txt) for create debezium connector.
  * Create below mentioned aliases:  
    * `export BOOTSTRAP_BROKERS_SASL_IAM=${CONNECT_BOOTSTRAP_SERVERS}`
    * `touch kafka.properties`
    * `alias kt='kafka-topics --bootstrap-server $BOOTSTRAP_BROKERS_SASL_IAM --command-config kafka.properties'`
    * `alias kcg='kafka-consumer-groups --bootstrap-server $BOOTSTRAP_BROKERS_SASL_IAM --command-config kafka.properties'`
    * `alias kacc='kafka-avro-console-consumer --consumer.config kafka.properties --bootstrap-server ${BOOTSTRAP_BROKERS_SASL_IAM} --property "schema.registry.url=${CONNECT_SCHEMA_REGISTRY_URL}" --property "print.key=true" --property "print.partition=true" --topic'`
    * `alias kcc='kafka-console-consumer --consumer.config kafka.properties --bootstrap-server ${BOOTSTRAP_BROKERS_SASL_IAM} --property "print.key=true" --property "print.partition=true" --topic'`
    * `alias kconfig='kafka-configs --bootstrap-server $BOOTSTRAP_BROKERS_SASL_IAM --command-config kafka.properties'`
    * `unset JAVA_TOOL_OPTIONS`
    * `unset KAFKA_LOG4J_OPTS`
* **Load data in source database**
  * Login to postgres container.
  * Login to Psql: `psql --host=127.0.0.1 --port=5432 --dbname=product_db --username=root`
  * Execute below insert command    
    `INSERT INTO catalog.outbox_event(id,"data","aggregate",aggregate_id,"source","type",schema_version,trace_id,"time")      
    VALUES(      
    '781d17ae-0c24-4b0c-88e4-e9d6f93bcc08'::uuid,'[{"fields":[{"name": "categoryHierarchyPathsForChannels", "type": "GLOBAL", "value":{"Deere.com": ["WG_products-and-solutions/WG_powergard"]}}, {"name":"categoryName", "type": "TRANSLATED", "value": {"en-US": "Power Gard"}}],"isDeleted":false}]',      
    'CATEGORY',      
    'WG_powergard',      
    'productApi',      
    'Category.Created',      
    'v1.0',      
    '66992c889ce9f9c622c430dfcf726f1b',      
    '2024-07-18 14:54:00.608'      
    );`
* **Verify in kafka-connect** by executing below command
  * `kacc productcatalog_outbox_event --from-beginning`
  * `kacc productcatalog_outbox_event`


## Kafka connect Use Cases
<ins>***Domain Events Go-Live***:  </ins>

Initially in PROD env we have kept the Domain event toggle **OFF**. In Domain events Go-Live in production we have re-publihsed all the domain events by using Ingester Admin API `/api/v1/admin/affectedEntitiesEvents/publish`
* **Steps for simulation**:
  * Clear outbox_event table.
  * Login to kafka-connect
  * Execute command to consume latest messages `kacc productcatalog_outbox_event`
* Execute insert statements ([see here](../connectors-guide/1-DE-Live/1-full-load-data.sql))
* Monitor and verify data using kafka avro console consumer.

<ins>***Restrict publishing Deletion events to Kafka***:  </ins>

We had to perform certain deletion queries in db tables, but to avoid publishing deletion events to kafka, we did certain changes in the debezium properties to restrict publishing deletion events.
* **Steps for simulation**:
  * Assuming we have some data in outbox_event table.  ([Ref](../connectors-guide/1-DE-Live/1-full-load-data.sql))
  * Login to kafka-connect
  * Execute command to consume latest messages `kacc productcatalog_outbox_event`
  * Delete any record from outbox_event table
  * Observe in avro console consumer delete event is publishing to kafka and able to consume.

* **Steps for mitigation**

  * Add below dependencies in **build.gradle**

    	  implementation "org.apache.groovy:groovy-jsr223:4.0.24"    
          implementation "org.apache.groovy:groovy:4.0.24"    
          implementation "io.debezium:debezium-scripting:2.6.1.Final"  
  * Update below mentioned Debezium connector properties
    * Update:  
      `"transforms":"compare,route,filter",`
    * Add:  
      *   `"transforms.filter.type": "io.debezium.transforms.Filter",`  
      *   `"transforms.filter.language": "jsr223.groovy",  `  
      *   `"transforms.filter.condition": "value.op != \"d\"",`

  * Build the project and place the jar file inside connect-jars directory.
  * Build and restart the kafka-connect container again.
  * Delete existing connector:
    * `curl -i -X DELETE http://localhost:8083/connectors/POSTGRES_OUTBOX_EVENT_SOURCE_CONNECTOR`
  * Create new connector with updated configs.
  * Execute command to consume latest messages `kacc productcatalog_outbox_event`
  * Try to delete another record
  * Observe no events should get published to kafka.

<ins>***Publishing of larger size messages***</ins>

Domain events produces certain larger size messages, which is bigger than the default max size message of the kafka and producer. due to this kafka connector failed to publishing the messages.
* **Steps for simulation**:
  * Assuming some messages are available in database.
  * exec kafka connect and start listening latest messages using kafka avro console consumer.
  * Insert certain records in outbox_event table which is greater than 1 MB. ([see here](../connectors-guide/3-DE-Process-large-message/large_record.sql))
  * Check the kafka avro console consumer records should not come
  * Check logs, error should get reported.
  * Connector got stopped and closed.
  
  
  * **Steps for mitigation**
      * Delete the connector using below command.
        * `curl -i -X DELETE http://localhost:8083/connectors/POSTGRES_OUTBOX_EVENT_SOURCE_CONNECTOR`
      * Increase kafka producer max record size to 2 MB. Add below config in kafka-connect service.
        * `CONNECT_PRODUCER_MAX_REQUEST_SIZE: "2097152"`
      * Increase kafka max record size to 2 MB. Add below config in all kafka brokers.
        * `KAFKA_MESSAGE_MAX_BYTES: 2097152`
      * Stop all the containers
        * `docker compose -f .\docker-compose-connect.yaml down`
      * Start all the containers again
        * `docker compose -f .\docker-compose-connect.yaml up -d`
      * Re-create the connector again.
      * Validate the logs, all records got processed without any error.
      * Try to insert another record with the larger size (>1 MB). ([see here](../connectors-guide/3-DE-Process-large-message/large_record_2.sql))


<ins>***Repartition of topic***</ins>

Currently the partition key is aggregateId + type, due to that for the same aggregateId events, messages can go in multiple partitions. In the consumer end they can read any partition first, due to that the ordering of messages for the same aggregateId can not be guaranteed.
* **Steps for simulation**:
  * Clear outbox_event table.
  * Start multiple kafka avro console consumers (3 consumers) with groupId test-consumer.
    * `kcc productcatalog_outbox_event --group group1`
  * Insert few records in outbox_event for certain product codes. ([see here](../connectors-guide/4-DE-Topic_Repartition/1-full-load-data.sql))
  * Validate that the same aggregateId, records are going in different partitions.
* **Steps for mitigation**
  * Update below mentioned Debezium connector properties
    * `"transforms":"compare,route,filter,partition-routing",`
  * Add below mentioned Debezium connector properties
    * `"transforms.partition-routing.type": "io.debezium.transforms.partitions.PartitionRouting",`
      `"transforms.partition-routing.partition.payload.fields":"change.aggregate_id",`
      `"transforms.partition-routing.partition.topic.num":"3",`
  * Build and copy the Jar
  * Delete the kafka connector
    * `curl -i -X DELETE http://localhost:8083/connectors/POSTGRES_OUTBOX_EVENT_SOURCE_CONNECTOR`
  * Stop the kafka connector service.
  * Cleanup of existing topic (delete topic using below command)
    * `kafka-topics --bootstrap-server kafka-broker-1:9092 --delete --topic productcatalog_outbox_event`
  * Build and copy the Jar
  * Start kafka connect service again.
    * `docker compose -f .\docker-compose-connect.yaml up -d kafka-connect`
  * Create kafka connector again.
    * Execute connector.txt
  * Publish few records in database.
    * We can delete all records from outbox_event table.
    * Insert all records. ([see here](../connectors-guide/4-DE-Topic_Repartition/1-full-load-data.sql))
  * Validate events by checking partitions data.
    * execute kafka command in 3 different window (consumer group with 3 instances).
      * `kcc productcatalog_outbox_event --group group1`

<ins>***Replay all messages***</ins>

After Domain events Go-Live we have performed replay all messages few times due to certain conditions (live re-partitions, cleanup of topic etc).
* **Steps for simulation**:
  * Assuming outbox_event table has certain records. If not insert all data. ([see here](../connectors-guide/5-DE-Replay/1-full-load-data.sql))  
  * Login to kafka-connect
  * Execute command to consume latest messages `kacc productcatalog_outbox_event`
  * Place a validator by using kafka avro console consumer, records should come once db updates happens.
    * `kacc productcatalog_outbox_event`
  * Execute update statements ([see here](../connectors-guide/5-DE-Replay/2-replay-full-load.sql))
  

