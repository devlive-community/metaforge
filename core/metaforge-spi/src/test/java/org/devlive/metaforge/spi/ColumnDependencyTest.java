package org.devlive.metaforge.spi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.devlive.metaforge.spi.generator.definition.ColumnDefinition;
import org.devlive.metaforge.spi.generator.definition.TableDefinition;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class ColumnDependencyTest
{
    private final ObjectMapper mapper = new ObjectMapper();
    private final List<String> executed = Lists.newArrayList();

    private ObjectNode row(String type, String name, String schema, String table)
    {
        return mapper.createObjectNode().put("object_type", type).put("object_name", name).put("object_schema", schema).put("object_table", table);
    }

    /** Answers the dependency lookups from canned rows and records every other statement. */
    private final PluginService service = new PluginService()
    {
        @Override
        public Response execute(RuntimeConfigure configure, String content)
        {
            List<Object> rows = Lists.newArrayList();
            if (content.contains("information_schema.STATISTICS")) {
                rows.add(row("index", "idx_status", "app", "flows"));
            }
            else if (content.contains("information_schema.KEY_COLUMN_USAGE")) {
                rows.add(row("foreign_key", "fk_child_status", "app", "child"));
            }
            else if (content.contains("information_schema.TRIGGERS")) {
                rows.add(row("trigger", "trg_flows", "app", "flows"));
            }
            else if (content.contains("information_schema.VIEWS")) {
                return Response.builder().isSuccessful(false).message("denied").build();
            }
            else if (content.contains("information_schema.COLUMNS")) {
                rows.add(row("other", "status_label", "app", "flows"));
            }
            else {
                executed.add(content);
            }
            return Response.builder().isSuccessful(true).isConnected(true).columns(rows).build();
        }
    };

    private TableDefinition definition(boolean force)
    {
        TableDefinition definition = TableDefinition.create("app", "flows");
        ColumnDefinition column = new ColumnDefinition();
        column.setName("status");
        definition.setColumns(Sets.newHashSet(column));
        definition.setForce(force);
        return definition;
    }

    @Test
    public void dependenciesMergeEveryReadableKind()
    {
        Response response = service.getColumnDependencies(new RuntimeConfigure(), definition(false));
        Assert.assertTrue(response.getIsSuccessful());
        Assert.assertEquals(4, response.getColumns().size());
        Assert.assertEquals("denied", response.getMessage());
        Assert.assertTrue(response.getContent().contains("COLUMN_NAME IN ('status')"));
        Assert.assertTrue(response.getContent().contains("LOCATE('`status`', ACTION_STATEMENT) > 0"));
        Assert.assertTrue(response.getContent().contains("LOCATE('`app`.`flows`', VIEW_DEFINITION) > 0"));
    }

    @Test
    public void forcedDropRemovesDependenciesFirst()
    {
        Response response = service.dropColumn(new RuntimeConfigure(), definition(true));
        Assert.assertTrue(response.getIsSuccessful());
        Assert.assertEquals(Lists.newArrayList(
                "ALTER TABLE `app`.`child` DROP FOREIGN KEY `fk_child_status`;",
                "DROP TRIGGER `app`.`trg_flows`;",
                "ALTER TABLE `app`.`flows`\n    DROP INDEX `idx_status`,\n    DROP COLUMN `status_label`,\n    DROP COLUMN `status`;"), executed);
        Assert.assertEquals(String.join("\n", executed), response.getContent());
    }

    @Test
    public void literalsAndIdentifiersAreEscaped()
    {
        Assert.assertEquals("'it''s \\\\ ok'", service.sqlLiteral("it's \\ ok"));
        Assert.assertEquals("`a``b`", service.sqlIdentifier("a`b"));
    }
}
