package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;
import java.util.List;

import static org.devlive.metaforge.test.e2e.PluginE2E.assumeDocker;
import static org.devlive.metaforge.test.e2e.PluginE2E.column;
import static org.devlive.metaforge.test.e2e.PluginE2E.configure;
import static org.devlive.metaforge.test.e2e.PluginE2E.first;
import static org.devlive.metaforge.test.e2e.PluginE2E.ok;

/**
 * Runs the Neo4j plugin through the Neo4j JDBC driver against every Neo4j release line it
 * documents as verified: nodes created with Cypher are listed as a database, labels and properties.
 */
@RunWith(Parameterized.class)
public class Neo4jE2ETest
{
    @Parameterized.Parameters(name = "Neo4j {0}")
    public static List<String> versions()
    {
        return List.of("4.4", "5");
    }

    private final String version;

    public Neo4jE2ETest(String version)
    {
        this.version = version;
    }

    @BeforeClass
    public static void docker()
    {
        assumeDocker();
    }

    @Test
    public void labelsAreListedAsTablesAndPropertiesAsColumns()
    {
        try (GenericContainer<?> container = new GenericContainer<>("neo4j:" + version)
                .withEnv("NEO4J_AUTH", "neo4j/password123")
                .withExposedPorts(7687)
                .waitingFor(Wait.forLogMessage(".*Started.*", 1).withStartupTimeout(Duration.ofMinutes(3)))) {
            container.start();
            PluginService service = new Neo4jService();
            RuntimeConfigure configure = configure(container.getHost(), container.getMappedPort(7687), "neo4j", "password123", "neo4j");
            configure.setType(service.connectType());

            String reported = first(ok("validator", service.execute(configure, service.validator())));
            Assert.assertTrue(reported + " is not " + version, reported.startsWith(version + "."));

            ok("create", service.execute(configure, "CREATE (:Person {name: 'Ada', born: 1815})-[:KNOWS]->(:Person {name: 'Alan'})"));
            Assert.assertEquals(List.of("neo4j"), column(ok("databases", service.getDatabases(configure)), "object_name"));
            Assert.assertTrue(column(ok("tables", service.getTables(configure, "neo4j")), "object_name").contains("Person"));
            List<String> properties = column(ok("columns", service.getColumns(configure, "neo4j", "Person")), "object_name");
            Assert.assertTrue(properties.toString(), properties.containsAll(List.of("name", "born")));
            Assert.assertEquals(List.of("Ada"), column(ok("query", service.execute(configure, "MATCH (p:Person) WHERE p.born = 1815 RETURN p.name AS name")), "name"));
        }
    }
}
