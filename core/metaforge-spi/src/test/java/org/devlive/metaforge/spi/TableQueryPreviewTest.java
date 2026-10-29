package org.devlive.metaforge.spi;

import org.devlive.metaforge.spi.generator.definition.TableDefinition;
import org.devlive.metaforge.spi.model.Pagination;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.junit.Assert;
import org.junit.Test;

public class TableQueryPreviewTest
{
    private final PluginService service = new PluginService()
    {
        @Override
        public Response execute(RuntimeConfigure configure, String content)
        {
            throw new AssertionError("SQL previews must not execute queries");
        }
    };

    @Test
    public void unpaginatedPreviewGeneratesFullDatasetSql()
    {
        TableDefinition definition = TableDefinition.create("analytics", "orders");
        definition.setPreview(true);
        Response response = service.queryTable(new RuntimeConfigure(), definition);
        Assert.assertTrue(response.getIsSuccessful());
        Assert.assertEquals("SELECT *\nFROM `analytics`.`orders`;", response.getContent());
        Assert.assertFalse(response.getContent().contains("LIMIT"));
    }

    @Test
    public void paginatedPreviewKeepsLimitWithoutExecutingCountQuery()
    {
        TableDefinition definition = TableDefinition.create("analytics", "orders");
        definition.setPreview(true);
        definition.setPagination(Pagination.create(20, 2));
        Response response = service.queryTable(new RuntimeConfigure(), definition);
        Assert.assertTrue(response.getContent().contains("LIMIT 20 OFFSET 20"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void realTableQueriesStillRequirePagination()
    {
        service.queryTable(new RuntimeConfigure(), TableDefinition.create("analytics", "orders"));
    }
}
