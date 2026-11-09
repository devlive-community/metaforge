package org.devlive.metaforge.spi;

import org.devlive.metaforge.spi.sql.SqlLimit;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

public class SqlLimitTest
{
    @Test
    public void plainQueriesGetALimit()
    {
        Assert.assertEquals(Optional.of("SELECT * FROM opsforge.logs\nLIMIT 1001"), SqlLimit.append("SELECT * FROM opsforge.logs;", 1001));
        Assert.assertEquals(Optional.of("select a from t order by a desc -- newest first\nLIMIT 5"), SqlLimit.append("select a from t order by a desc -- newest first", 5));
        Assert.assertEquals(Optional.of("WITH x AS (SELECT 1 AS a LIMIT 3) SELECT * FROM x UNION ALL SELECT 2\nLIMIT 5"),
                SqlLimit.append("WITH x AS (SELECT 1 AS a LIMIT 3) SELECT * FROM x UNION ALL SELECT 2", 5));
        Assert.assertEquals(Optional.of("SELECT 'limit 3', `limit` FROM t WHERE id IN (SELECT id FROM u LIMIT 2)\nLIMIT 5"),
                SqlLimit.append("SELECT 'limit 3', `limit` FROM t WHERE id IN (SELECT id FROM u LIMIT 2)", 5));
    }

    @Test
    public void statementsWithTheirOwnRowClauseStayAsWritten()
    {
        for (String sql : new String[] {
                "SELECT * FROM t LIMIT 10",
                "SELECT * FROM t ORDER BY a OFFSET 5 ROWS FETCH NEXT 10 ROWS ONLY",
                "SELECT * FROM t OFFSET 5",
                "SELECT * FROM t FOR UPDATE",
                "SELECT * FROM t LOCK IN SHARE MODE",
                "SELECT * INTO OUTFILE '/tmp/x' FROM t",
                "SELECT * FROM t SETTINGS max_threads = 1",
                "SELECT * FROM t FORMAT JSON",
                "WITH x AS (SELECT 1) INSERT INTO t SELECT * FROM x"}) {
            Assert.assertEquals(sql, Optional.empty(), SqlLimit.append(sql, 5));
        }
    }

    @Test
    public void otherStatementsAndScriptsStayAsWritten()
    {
        for (String sql : new String[] {"SHOW TABLES", "DESCRIBE t", "EXPLAIN SELECT * FROM t", "UPDATE t SET a = 1", "SELECT 1; SELECT 2", "", "SELECT (1"}) {
            Assert.assertEquals(sql, Optional.empty(), SqlLimit.append(sql, 5));
        }
        Assert.assertEquals(Optional.empty(), SqlLimit.append("SELECT 1", 0));
    }
}
