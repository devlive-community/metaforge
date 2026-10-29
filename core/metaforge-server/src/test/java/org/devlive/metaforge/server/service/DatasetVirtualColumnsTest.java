package org.devlive.metaforge.server.service;

import org.devlive.metaforge.common.sql.SqlBuilder;
import org.devlive.metaforge.common.sql.configure.SqlBody;
import org.devlive.metaforge.common.sql.configure.SqlColumn;
import org.devlive.metaforge.common.sql.configure.SqlOperator;
import org.devlive.metaforge.common.sql.configure.SqlType;
import org.devlive.metaforge.service.dataset.DatasetVirtualColumns;
import org.devlive.metaforge.service.entity.DataSetColumnEntity;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DatasetVirtualColumnsTest
{
    private DataSetColumnEntity column(long id, String name, String expression)
    {
        return DataSetColumnEntity.builder().id(id).name(name).virtualColumn(expression != null).formula(expression).build();
    }

    @Test
    public void acceptsNestedScalarFunctionsAndPhysicalFieldReferences()
    {
        String expression = DatasetVirtualColumns.normalize("round(abs(`amount`) * 1.1, 2)", Set.of("amount"));
        assertTrue(expression.contains("round"));
        DatasetVirtualColumns.validate(List.of(column(1, "amount", null), column(2, "net", expression)));
    }

    @Test
    public void rejectsUnknownFieldsVirtualDependenciesStatementsAliasesAndAggregations()
    {
        for (String expression : new String[] {"", "missing * 2", "other_virtual + 1", "other.amount", "amount; DELETE FROM orders", "amount AS renamed", "(SELECT amount FROM orders)", "sum(amount)", "count(amount)", "sumIf(amount, amount > 0)"}) {
            assertThrows(IllegalArgumentException.class, () -> DatasetVirtualColumns.normalize(expression, Set.of("amount")), expression);
        }
    }

    @Test
    public void commentsCannotSwallowGeneratedSqlAndStringsRemainIntact()
    {
        String expression = DatasetVirtualColumns.normalize("concat('x -- note', `name`) -- trailing comment", Set.of("name"));
        assertTrue(expression.contains("'x -- note'"));
        assertFalse(expression.contains("trailing comment"));
    }

    @Test
    public void virtualFunctionsExecuteInRealSqlAndRemainAvailableToOuterFilters()
    {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", ""));
        try {
            jdbc.execute("CREATE TABLE orders (amount DOUBLE, name VARCHAR(40))");
            jdbc.update("INSERT INTO orders VALUES (10, 'alice'), (3, 'bob')");
            List<DataSetColumnEntity> columns = List.of(column(1, "amount", null), column(2, "name", null),
                    column(3, "net", "round(amount * 1.1, 2)"), column(4, "display_name", "upper(name)"));
            String table = DatasetVirtualColumns.table("public", "orders", columns, Set.of(3L, 4L));
            String sql = new SqlBuilder(SqlBody.builder().type(SqlType.SELECT).table(table)
                    .columns(List.of(SqlColumn.builder().column("net").build(), SqlColumn.builder().column("display_name").build()))
                    .where(List.of(SqlColumn.builder().column("net").operator(SqlOperator.GT).value(10).build())).condition(" AND ").limit(100).build()).getSql();
            assertEquals(11.0, ((Number) jdbc.queryForList(sql).get(0).get("net")).doubleValue(), 0.00001);
            assertEquals("ALICE", jdbc.queryForList(sql).get(0).get("display_name"));
            String aggregation = "SELECT SUM(net) FROM " + table;
            assertEquals(14.3, jdbc.queryForObject(aggregation, Double.class), 0.00001);
        }
        finally {
            jdbc.execute("SHUTDOWN");
        }
    }

    @Test
    public void physicalOnlyQueriesAndLegacyBlankVirtualColumnsKeepTheirExistingPath()
    {
        assertEquals(null, DatasetVirtualColumns.table("db", "orders", List.of(column(1, "amount", null)), Set.of(1L)));
        DataSetColumnEntity legacy = column(2, "old_virtual", "");
        assertEquals(null, DatasetVirtualColumns.table("db", "orders", List.of(column(1, "amount", null), legacy), Set.of(1L)));
        assertThrows(IllegalArgumentException.class, () -> DatasetVirtualColumns.validate(List.of(legacy)));
    }
    @Test
    public void rawDatasetPreviewPreservesDuplicateRowsAndDoesNotAggregate()
    {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", ""));
        try {
            jdbc.execute("CREATE TABLE orders (amount DOUBLE)");
            jdbc.update("INSERT INTO orders VALUES (10), (10)");
            String sql = DatasetVirtualColumns.previewSql("public", "orders", List.of(column(1, "amount", null), column(2, "net", "round(amount * 1.1, 2)")));
            assertFalse(sql.contains("GROUP BY"));
            assertTrue(sql.contains("LIMIT 100"));
            assertEquals(2, jdbc.queryForList(sql).size());
        }
        finally {
            jdbc.execute("SHUTDOWN");
        }
    }

}
