package org.devlive.metaforge.spi;

import com.google.common.collect.Lists;
import org.devlive.metaforge.spi.generator.definition.TableDefinition;
import org.devlive.metaforge.spi.generator.definition.TableOptionsDefinition;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class DatabaseOptionsStatementTest
{
    private final List<String> executed = Lists.newArrayList();

    private final PluginService service = new PluginService()
    {
        @Override
        public Response execute(RuntimeConfigure configure, String content)
        {
            executed.add(content);
            return Response.builder().isSuccessful(true).columns(Lists.newArrayList()).build();
        }
    };

    private static TableDefinition definition(String charset, String collate, boolean preview)
    {
        TableOptionsDefinition options = new TableOptionsDefinition();
        options.setCharset(charset);
        options.setCollate(collate);
        TableDefinition definition = TableDefinition.create("app", null);
        definition.setOptions(options);
        definition.setPreview(preview);
        return definition;
    }

    @Test
    public void previewBuildsOneAlterDatabaseWithoutRunningIt()
    {
        Response response = service.alterDatabaseOptions(new RuntimeConfigure(), definition("utf8mb4", "utf8mb4_bin", true));
        Assert.assertEquals("ALTER DATABASE `app` DEFAULT CHARACTER SET = utf8mb4 DEFAULT COLLATE = utf8mb4_bin;", response.getContent());
        Assert.assertTrue(executed.isEmpty());
    }

    @Test
    public void statementsQueryTheSchemaAsALiteral()
    {
        service.getDatabase(new RuntimeConfigure(), "a'b");
        service.getDatabaseTables(new RuntimeConfigure(), "a'b");
        Assert.assertTrue(executed.get(0).contains("WHERE s.SCHEMA_NAME = 'a''b'"));
        Assert.assertTrue(executed.get(0).contains("information_schema.EVENTS WHERE EVENT_SCHEMA = 'a''b'"));
        Assert.assertTrue(executed.get(1).contains("TABLE_SCHEMA = 'a''b' AND TABLE_TYPE = 'BASE TABLE'"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void optionsAreSingleWords()
    {
        service.alterDatabaseOptions(new RuntimeConfigure(), definition("utf8mb4; DROP", null, true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void somethingMustChange()
    {
        service.alterDatabaseOptions(new RuntimeConfigure(), definition(null, null, true));
    }
}
