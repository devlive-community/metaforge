package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.devlive.metaforge.spi.sql.SqlLimit;
import org.junit.Assert;
import org.junit.Test;

import static org.devlive.metaforge.test.e2e.PluginE2E.configure;
import static org.devlive.metaforge.test.e2e.PluginE2E.ok;

/**
 * The query page's row cap on a real engine: the LIMIT written into the SQL stops the database at
 * cap + 1 rows, and the adapter returns the first cap rows marked as truncated. A result within the
 * cap comes back whole and unmarked; a statement with no cap reads every row.
 */
public class H2RowLimitTest
{
    @Test
    public void longResultsAreCutAndMarked()
    {
        PluginService service = new H2Service();
        RuntimeConfigure setup = configure("-", 1, "sa", null, "limits");
        ok("create table", service.execute(setup, "CREATE TABLE logs AS SELECT X AS id FROM SYSTEM_RANGE(1, 50)"));

        RuntimeConfigure capped = configure("-", 1, "sa", null, "limits");
        capped.setMaxRows(10);
        String statement = SqlLimit.append("SELECT id FROM logs ORDER BY id", 11).orElseThrow();
        Response cut = ok("capped select", service.execute(capped, statement));
        Assert.assertEquals(10, cut.getColumns().size());
        Assert.assertEquals(Boolean.TRUE, cut.getTruncated());

        // Without the LIMIT the JDBC cap alone still bounds the result.
        Response driverCut = ok("driver capped select", service.execute(capped, "SELECT id FROM logs"));
        Assert.assertEquals(10, driverCut.getColumns().size());
        Assert.assertEquals(Boolean.TRUE, driverCut.getTruncated());

        Response whole = ok("short select", service.execute(capped, "SELECT id FROM logs WHERE id <= 10"));
        Assert.assertEquals(10, whole.getColumns().size());
        Assert.assertNull(whole.getTruncated());

        Response all = ok("uncapped select", service.execute(configure("-", 1, "sa", null, "limits"), "SELECT id FROM logs"));
        Assert.assertEquals(50, all.getColumns().size());
        Assert.assertNull(all.getTruncated());

        ok("drop table", service.execute(setup, "DROP TABLE logs"));
    }
}
