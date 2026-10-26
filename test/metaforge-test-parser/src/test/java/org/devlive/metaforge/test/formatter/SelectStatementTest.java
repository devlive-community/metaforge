package org.devlive.metaforge.test.formatter;

import org.devlive.metaforge.sql.SQLParser;
import org.devlive.metaforge.sql.formatter.SelectFormatter;
import org.devlive.metaforge.sql.statement.SQLStatement;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class SelectStatementTest
{
    @Test
    public void test()
    {
        String sql = "SELECT id, name, COUNT(1) as count FROM users WHERE age > 18 and name = 12 GROUP BY id, name HAVING count > 1 ORDER BY id DESC LIMIT 10";
        SQLStatement statement = SQLParser.parse(sql);
        SelectFormatter formatter = new SelectFormatter();
        assertNotNull(formatter.format(statement));
    }
}
