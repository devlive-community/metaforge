package org.devlive.metaforge.service.service.impl;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReadOnlyChartSqlTest
{
    @Test
    public void acceptsOneSelectWithAggregatesAndQuotedDelimiters()
    {
        for (String sql : List.of(
                " SELECT COUNT(*) AS total FROM `events` WHERE message = 'it''s a ; -- value'; ",
                "SELECT CAST(score AS DECIMAL(10, 2)) FROM \"scores\" WHERE score IN (1, 2)",
                "SELECT DISTINCT account_id FROM events WHERE EXISTS (SELECT 1 FROM accounts)")) {
            assertTrue(sql, ReadOnlyChartSql.accepts(sql));
        }
    }

    @Test
    public void rejectsScriptsWritesAndCallsWithSideEffects()
    {
        for (String sql : List.of(
                "DELETE FROM events",
                "SELECT * FROM events; DELETE FROM events",
                "SELECT 'unclosed",
                "SELECT 'escape\\' ; SELECT 2",
                "SELECT * FROM events FOR UPDATE",
                "SELECT * INTO OUTFILE '/tmp/events' FROM events",
                "SELECT * INTO new_table FROM events",
                "SELECT /*!50000 GET_LOCK('chart', 0) */ 1",
                "SELECT get_lock('chart', 0)",
                "SELECT pg_catalog.count(id) FROM events",
                "SELECT \"count\"(id) FROM events",
                "SELECT @variable := 1",
                "SELECT $$unexpected$$")) {
            assertFalse(sql, ReadOnlyChartSql.accepts(sql));
        }
    }
}
