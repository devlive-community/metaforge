package org.devlive.metaforge.spi;

import com.google.common.collect.Lists;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class DatabaseErTest
{
    private final List<String> executed = Lists.newArrayList();

    private final PluginService service = new PluginService()
    {
        @Override
        public Response execute(RuntimeConfigure configure, String content)
        {
            executed.add(content);
            if (content.contains("KEY_COLUMN_USAGE")) {
                return Response.builder().isSuccessful(false).message("denied").build();
            }
            List<Object> rows = new ArrayList<>();
            rows.add(content.contains("information_schema.COLUMNS") ? "column" : "table");
            return Response.builder().isSuccessful(true).columns(rows).build();
        }
    };

    @Test
    public void tablesColumnsAndForeignKeysOfOneSchemaAreRead()
    {
        Response response = service.getDatabaseEr(new RuntimeConfigure(), "a'b");
        Assert.assertEquals(3, executed.size());
        Assert.assertTrue(executed.get(0).contains("TABLE_SCHEMA = 'a''b' AND TABLE_TYPE = 'BASE TABLE'"));
        Assert.assertTrue(executed.get(2).contains("REFERENCED_TABLE_SCHEMA = 'a''b'"));
        Assert.assertEquals(2, response.getColumns().size());
        Assert.assertEquals("denied", response.getMessage());
    }
}
