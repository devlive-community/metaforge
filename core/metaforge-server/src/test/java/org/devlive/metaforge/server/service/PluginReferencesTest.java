package org.devlive.metaforge.server.service;

import org.devlive.metaforge.plugin.PluginMetadata;
import org.devlive.metaforge.plugin.PluginType;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType.H2;

public class PluginReferencesTest
{
    @Test
    public void countsTrackedReferencesAndGuardsUninstall()
    {
        var database = new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(H2).build();
        try {
            var jdbc = new JdbcTemplate(database);
            jdbc.execute("CREATE TABLE metaforge_source (name VARCHAR(64), source_type VARCHAR(64))");
            jdbc.execute("CREATE TABLE metaforge_dataset (name VARCHAR(64), executor VARCHAR(64), scheduler VARCHAR(64))");
            jdbc.execute("CREATE TABLE metaforge_workflow (name VARCHAR(64), executor VARCHAR(64))");
            jdbc.update("INSERT INTO metaforge_source VALUES (?, ?)", "Production", "MYSQL");
            jdbc.update("INSERT INTO metaforge_dataset VALUES (?, ?, ?)", "Daily dataset", "Local", "LocalScheduler");
            jdbc.update("INSERT INTO metaforge_workflow VALUES (?, ?)", "Daily workflow", "LocalExecutor");
            var plugins = List.of(plugin("MySQL", PluginType.CONNECTOR), plugin("Redis", PluginType.CONNECTOR),
                    plugin("MetaForge", PluginType.EXECUTOR), plugin("LocalScheduler", PluginType.SCHEDULER), plugin("JsonConvert", PluginType.CONVERT));

            var service = new PluginReferences(jdbc);
            var listed = service.list(plugins);
            assertEquals("Production", listed.get("MySQL").get(0).getName());
            assertEquals(2, listed.get("MetaForge").size());
            assertEquals("dataset", listed.get("LocalScheduler").get(0).getKind());
            assertTrue(listed.get("Redis").isEmpty());
            assertFalse(listed.containsKey("JsonConvert"));

            assertTrue(service.isInUse("MySQL", plugins));
            assertTrue(service.isInUse("MetaForge", plugins));
            assertTrue(service.isInUse("LocalScheduler", plugins));
            assertFalse(service.isInUse("Redis", plugins));

            jdbc.update("DELETE FROM metaforge_source");
            assertFalse(service.isInUse("MySQL", plugins));
        }
        finally {
            database.shutdown();
        }
    }

    private static PluginMetadata plugin(String name, PluginType type)
    {
        return PluginMetadata.builder().name(name).type(type).build();
    }
}
