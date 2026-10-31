package org.devlive.metaforge;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.FinalizedVersionRange;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.devlive.metaforge.driver.sql.ConnectionUrl;
import org.devlive.metaforge.driver.sql.SourceColumn;
import org.devlive.metaforge.driver.sql.SqlSource;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/** A Kafka cluster as the database {@code kafka}: topics are tables, records are rows. */
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"}, justification = "The source owns its Kafka clients")
public final class KafkaSource
        implements SqlSource
{
    public static final String DATABASE = "kafka";
    static final List<SourceColumn> COLUMNS = List.of(
            new SourceColumn("partition", "INTEGER", false, true, "Partition of the record"),
            new SourceColumn("offset", "BIGINT", false, true, "Offset of the record in its partition"),
            new SourceColumn("timestamp", "TIMESTAMP", false, false, "Record timestamp"),
            new SourceColumn("key", "VARCHAR", true, false, "Record key as UTF-8 text"),
            new SourceColumn("value", "VARCHAR", true, false, "Record value as UTF-8 text"));
    private static final Set<String> DRIVER_OPTIONS = Set.of("timeout", "maxRows", "user", "password");

    private final Admin admin;
    private final Properties properties;
    private final long timeout;
    private final long maxRows;

    private KafkaSource(Admin admin, Properties properties, long timeout, long maxRows)
    {
        this.admin = admin;
        this.properties = properties;
        this.timeout = timeout;
        this.maxRows = maxRows;
    }

    static KafkaSource connect(ConnectionUrl url)
            throws SQLException
    {
        Properties properties = new Properties();
        url.getParameters().forEach((key, value) -> {
            if (!DRIVER_OPTIONS.contains(key)) {
                properties.put(key, value);
            }
        });
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, url.getHosts());
        long timeout = Long.parseLong(url.get("timeout", "10000"));
        properties.putIfAbsent(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, String.valueOf(timeout));
        properties.putIfAbsent(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, String.valueOf(timeout));
        Admin admin = Admin.create(properties);
        KafkaSource source = new KafkaSource(admin, properties, timeout, Long.parseLong(url.get("maxRows", "10000")));
        try {
            source.await(admin.describeCluster().clusterId(), "Connect");
        }
        catch (SQLException error) {
            admin.close(Duration.ofSeconds(1));
            throw error;
        }
        return source;
    }

    private <T> T await(Future<T> future, String what)
            throws SQLException
    {
        try {
            return future.get(timeout, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new SQLException(what + " was interrupted", error);
        }
        catch (ExecutionException error) {
            Throwable cause = error.getCause() == null ? error : error.getCause();
            throw new SQLException(what + " failed: " + cause.getMessage(), cause);
        }
        catch (TimeoutException error) {
            throw new SQLException(what + " did not finish within " + timeout + " ms", error);
        }
    }

    private static void inKafka(String database)
            throws SQLException
    {
        if (!DATABASE.equals(database)) {
            throw new SQLException("Unknown database " + database + "; a Kafka cluster is the database " + DATABASE);
        }
    }

    @Override
    public String productName()
    {
        return "Kafka";
    }

    /** The release the cluster speaks: inter.broker.protocol.version where brokers report it, metadata.version otherwise. */
    @Override
    public String productVersion()
            throws SQLException
    {
        Collection<Node> nodes = await(admin.describeCluster().nodes(), "Describe cluster");
        if (!nodes.isEmpty()) {
            ConfigResource broker = new ConfigResource(ConfigResource.Type.BROKER, String.valueOf(nodes.iterator().next().id()));
            Config config = await(admin.describeConfigs(List.of(broker)).values().get(broker), "Describe broker");
            ConfigEntry protocol = config.get("inter.broker.protocol.version");
            if (protocol != null && protocol.value() != null && !protocol.value().isEmpty()) {
                return protocol.value();
            }
        }
        FinalizedVersionRange metadata = await(admin.describeFeatures().featureMetadata(), "Describe features").finalizedFeatures().get("metadata.version");
        return metadata == null ? "unknown" : "metadata.version " + metadata.maxVersionLevel();
    }

    @Override
    public List<String> databases()
    {
        return List.of(DATABASE);
    }

    @Override
    public List<String> tables(String database)
            throws SQLException
    {
        inKafka(database);
        return await(admin.listTopics().names(), "List topics").stream().sorted().collect(Collectors.toList());
    }

    @Override
    public void createTable(String database, String table, List<SourceColumn> columns)
            throws SQLException
    {
        createTable(database, table, columns, Map.of());
    }

    /** WITH ('partitions' = '3', 'replication.factor' = '1', 'retention.ms' = '...'): other options become topic configs. */
    @Override
    public void createTable(String database, String table, List<SourceColumn> columns, Map<String, String> options)
            throws SQLException
    {
        inKafka(database);
        Set<String> fixed = COLUMNS.stream().map(SourceColumn::getName).collect(Collectors.toSet());
        for (SourceColumn column : columns) {
            if (!fixed.contains(column.getName())) {
                throw new SQLException("A topic's columns are fixed (" + String.join(", ", fixed) + "), not " + column.getName());
            }
        }
        Map<String, String> configs = new HashMap<>(options);
        int partitions = Integer.parseInt(configs.getOrDefault("partitions", "1"));
        short replication = Short.parseShort(configs.getOrDefault("replication.factor", "1"));
        configs.remove("partitions");
        configs.remove("replication.factor");
        await(admin.createTopics(List.of(new NewTopic(table, partitions, replication).configs(configs))).all(), "CREATE TABLE");
    }

    @Override
    public void dropTable(String database, String table)
            throws SQLException
    {
        inKafka(database);
        await(admin.deleteTopics(List.of(table)).all(), "DROP TABLE");
    }

    @Override
    public List<SourceColumn> columns(String database, String table)
            throws SQLException
    {
        inKafka(database);
        return COLUMNS;
    }

    private Properties clientProperties()
    {
        Properties client = new Properties();
        client.putAll(properties);
        client.remove(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG);
        return client;
    }

    /** Reads the records present when the statement starts, every partition from its beginning, up to maxRows. */
    @Override
    public List<List<Object>> rows(String database, String table, long limit)
            throws SQLException
    {
        inKafka(database);
        TopicDescription topic = await(admin.describeTopics(List.of(table)).allTopicNames(), "Describe topic").get(table);
        List<TopicPartition> partitions = topic.partitions().stream().map(info -> new TopicPartition(table, info.partition())).collect(Collectors.toList());
        long wanted = limit < 0 ? maxRows : Math.min(limit, maxRows);
        Properties client = clientProperties();
        client.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        client.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        client.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        List<List<Object>> rows = new ArrayList<>();
        try (KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<>(client)) {
            consumer.assign(partitions);
            consumer.seekToBeginning(partitions);
            Map<TopicPartition, Long> end = consumer.endOffsets(partitions, Duration.ofMillis(timeout));
            long deadline = System.currentTimeMillis() + timeout;
            while (rows.size() < wanted && partitions.stream().anyMatch(partition -> consumer.position(partition) < end.get(partition))) {
                if (System.currentTimeMillis() > deadline) {
                    throw new SQLException("Reading " + table + " did not finish within " + timeout + " ms");
                }
                for (ConsumerRecord<byte[], byte[]> record : consumer.poll(Duration.ofMillis(500))) {
                    if (record.offset() < end.get(new TopicPartition(table, record.partition())) && rows.size() < wanted) {
                        rows.add(Arrays.asList(record.partition(), record.offset(), new Timestamp(record.timestamp()),
                                text(record.key()), text(record.value())));
                    }
                }
            }
        }
        catch (RuntimeException error) {
            throw new SQLException("Reading " + table + " failed: " + error.getMessage(), error);
        }
        return rows;
    }

    private static String text(byte[] bytes)
    {
        return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
    }

    /** Produces one record per row: key and value, and optionally partition and timestamp. */
    @Override
    public int insert(String database, String table, List<String> columns, List<List<Object>> rows)
            throws SQLException
    {
        inKafka(database);
        if (columns.contains("offset")) {
            throw new SQLException("Kafka assigns offsets; leave out the offset column");
        }
        Properties client = clientProperties();
        client.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        client.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        client.putIfAbsent(ProducerConfig.ACKS_CONFIG, "all");
        try (KafkaProducer<byte[], byte[]> producer = new KafkaProducer<>(client)) {
            List<Future<RecordMetadata>> sent = new ArrayList<>();
            for (List<Object> row : rows) {
                Map<String, Object> values = new HashMap<>();
                for (int i = 0; i < columns.size(); i++) {
                    values.put(columns.get(i), row.get(i));
                }
                Integer partition = values.get("partition") == null ? null : Integer.valueOf(String.valueOf(values.get("partition")));
                Long timestamp = values.get("timestamp") == null ? null : timestamp(values.get("timestamp"));
                sent.add(producer.send(new ProducerRecord<>(table, partition, timestamp, bytes(values.get("key")), bytes(values.get("value")))));
            }
            producer.flush();
            for (Future<RecordMetadata> future : sent) {
                await(future, "INSERT");
            }
            return rows.size();
        }
        catch (RuntimeException error) {
            throw new SQLException("INSERT failed: " + error.getMessage(), error);
        }
    }

    private static long timestamp(Object value)
    {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        String text = String.valueOf(value);
        return text.matches("\\d+") ? Long.parseLong(text) : Timestamp.valueOf(text).getTime();
    }

    private static byte[] bytes(Object value)
    {
        return value == null ? null : String.valueOf(value).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void close()
    {
        admin.close(Duration.ofSeconds(5));
    }
}
