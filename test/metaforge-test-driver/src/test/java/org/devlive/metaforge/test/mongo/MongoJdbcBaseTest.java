package org.devlive.metaforge.test.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

@Slf4j
public abstract class MongoJdbcBaseTest
{
    // Static container instance shared by all test classes
    public static final GenericContainer<?> MONGO_CONTAINER = new GenericContainer(DockerImageName.parse("mongo"))
            .withExposedPorts(27017)
            .withEnv("MONGO_INITDB_ROOT_USERNAME", "mongoadmin")
            .withEnv("MONGO_INITDB_ROOT_PASSWORD", "secret")
            .withCommand("mongod", "--noauth")
            .waitingFor(Wait.forListeningPort()
                    .withStartupTimeout(Duration.ofSeconds(30)))
            .withCommand("mongod", "--auth");
    protected Statement statement;
    protected Connection connection;
    protected MongoClient mongoClient;

    @BeforeClass
    public static void assumeDockerAvailable()
    {
        // 本地无 Docker 环境时跳过容器集成测试，CI（带 Docker）仍然执行
        Assume.assumeTrue("Docker is not available, skipping Mongo container tests",
                MONGO_CONTAINER.isRunning());
    }

    @Before
    public void init()
    {
        try {
            initializeMongoData();
            initializeJdbcConnection();
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to initialize test environment", e);
        }
    }

    private void initializeMongoData()
    {
        String mongoUri = String.format("mongodb://mongoadmin:secret@%s:%d",
                MONGO_CONTAINER.getHost(),
                MONGO_CONTAINER.getFirstMappedPort());
        mongoClient = MongoClients.create(mongoUri);

        // Clean up existing data
        MongoDatabase database = mongoClient.getDatabase("test");
        try {
            database.getCollection("sample").drop();
        }
        catch (Exception ignored) {
        }

        database.createCollection("sample");
        database.getCollection("sample").insertMany(
                List.of(
                        new Document("name", "test1").append("value", 1),
                        new Document("name", "test2").append("value", 2),
                        new Document("name", "test3").append("value", 3),
                        new Document("name", "test1").append("value", 4),
                        new Document("name", "test2").append("value", 2)
                )
        );
    }

    private void initializeJdbcConnection()
            throws Exception
    {
        Class.forName("org.devlive.metaforge.driver.MongoJdbcDriver");
        Properties props = new Properties();
        props.setProperty("database", "admin");
        props.setProperty("user", "mongoadmin");
        props.setProperty("password", "secret");

        String jdbcUrl = String.format("jdbc:mongodb://%s:%d",
                MONGO_CONTAINER.getHost(),
                MONGO_CONTAINER.getFirstMappedPort()
        );
        connection = DriverManager.getConnection(jdbcUrl, props);
        statement = connection.createStatement();
    }

    @After
    public void cleanup()
    {
        try {
            if (statement != null) {
                statement.close();
            }
            if (connection != null) {
                connection.close();
            }
            if (mongoClient != null) {
                mongoClient.close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    static {
        // 无 Docker 时跳过启动，交由 assumeDockerAvailable 标记跳过
        if (isDockerAvailable()) {
            MONGO_CONTAINER.start();
        }
    }

    private static boolean isDockerAvailable()
    {
        // Docker Desktop（Engine 29）会以 HTTP 400 拒绝 docker-java 默认 API 版本，钉一个双方都支持的版本
        if (System.getProperty("api.version") == null && System.getenv("DOCKER_API_VERSION") == null) {
            System.setProperty("api.version", "1.41");
        }
        try {
            return org.testcontainers.DockerClientFactory.instance().isDockerAvailable();
        }
        catch (Throwable ex) {
            return false;
        }
    }
}
