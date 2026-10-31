package org.devlive.metaforge.spi;

import com.google.common.collect.Sets;
import org.devlive.metaforge.spi.generator.Filter;
import org.devlive.metaforge.spi.generator.definition.ColumnDefinition;
import org.devlive.metaforge.spi.generator.definition.TableDefinition;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class TableQueryFilterTest
{
    private final PluginService service = new PluginService() {};

    private static ColumnDefinition filter(String name, Filter.Operator operator, Object value)
    {
        ColumnDefinition column = new ColumnDefinition();
        column.setName(name);
        column.setOperator(operator);
        column.setValue(value);
        return column;
    }

    @Test
    public void inFiltersFormatJsonArrays()
    {
        Assert.assertEquals("`type` IN ('SYSTEM', 'O''NEIL')", Filter.create("type", Filter.Operator.IN, Arrays.asList("SYSTEM", "O'NEIL")).build());
    }

    @Test
    public void countClauseMatchesTheDataFilters()
    {
        TableDefinition definition = TableDefinition.create("analytics", "orders");
        Assert.assertEquals("", service.formatFilterClause(definition));
        // A date range sends two filters on the same column; both must survive the set.
        definition.setFilters(Sets.newLinkedHashSet(Arrays.asList(
                filter("created_at", Filter.Operator.GE, "2026-10-01 00:00:00"),
                filter("created_at", Filter.Operator.LE, "2026-10-31 23:59:59"))));
        Assert.assertEquals("\n    WHERE `created_at` >= '2026-10-01 00:00:00' AND `created_at` <= '2026-10-31 23:59:59'", service.formatFilterClause(definition));
    }
}
