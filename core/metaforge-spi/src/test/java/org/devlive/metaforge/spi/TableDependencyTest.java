package org.devlive.metaforge.spi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TableDependencyTest
{
    private final List<String> executed = Lists.newArrayList();

    /** Answers the foreign key and view queries with one row each, and fails the routine query. */
    private final PluginService service = new PluginService()
    {
        @Override
        public Response execute(RuntimeConfigure configure, String content)
        {
            executed.add(content);
            if (content.contains("information_schema.ROUTINES")) {
                return Response.builder().isSuccessful(false).message("denied").build();
            }
            List<Object> rows = new ArrayList<>();
            if (content.contains("KEY_COLUMN_USAGE") || content.contains("information_schema.VIEWS")) {
                rows.add(new ObjectMapper().createObjectNode().put("object_type", content.contains("VIEWS") ? "view" : "foreign_key"));
            }
            return Response.builder().isSuccessful(true).columns(rows).build();
        }
    };

    @Test
    public void everyKindIsCheckedAndFailuresAreReportedWithoutLosingTheRest()
    {
        Response response = service.getTableDependencies(new RuntimeConfigure(), "app", "user");
        Assert.assertEquals(4, executed.size());
        Assert.assertTrue(response.getIsSuccessful());
        Assert.assertEquals(2, response.getColumns().size());
        Assert.assertEquals("denied", response.getMessage());
    }

    @Test
    public void foreignKeysOfTheTableItselfAreLeftOut()
    {
        service.getTableDependencies(new RuntimeConfigure(), "app", "user");
        Assert.assertTrue(executed.get(0).contains("REFERENCED_TABLE_SCHEMA = 'app' AND REFERENCED_TABLE_NAME = 'user'"));
        Assert.assertTrue(executed.get(0).contains("AND NOT (TABLE_SCHEMA = 'app' AND TABLE_NAME = 'user')"));
        Assert.assertTrue(executed.get(1).contains("LOCATE('`app`.`user`', VIEW_DEFINITION) > 0"));
    }
}
