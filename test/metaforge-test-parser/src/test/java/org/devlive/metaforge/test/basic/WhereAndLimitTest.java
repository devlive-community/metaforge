package org.devlive.metaforge.test.basic;

import org.devlive.metaforge.sql.SQLParser;
import org.devlive.metaforge.sql.node.Expression;
import org.devlive.metaforge.sql.statement.SelectStatement;
import org.junit.Assert;
import org.junit.Test;

public class WhereAndLimitTest
{
    private static SelectStatement select(String sql)
    {
        return (SelectStatement) SQLParser.parse(sql);
    }

    @Test
    public void limitAndOffsetKeepTheirMeaningInBothForms()
    {
        SelectStatement standard = select("SELECT * FROM t LIMIT 5 OFFSET 2");
        Assert.assertEquals(5, standard.getLimitClause().getLimit());
        Assert.assertEquals(2, standard.getLimitClause().getOffset());
        SelectStatement mysql = select("SELECT * FROM t LIMIT 2, 5");
        Assert.assertEquals(5, mysql.getLimitClause().getLimit());
        Assert.assertEquals(2, mysql.getLimitClause().getOffset());
    }

    @Test
    public void likeNotInAndNullTestsAreKept()
    {
        Expression where = select("SELECT * FROM t WHERE a LIKE 'x%' OR NOT b IN (1, 2) AND c IS NOT NULL").getWhereClause();
        Assert.assertEquals("OR", where.getValue());
        Expression like = where.getChildren().get(0);
        Assert.assertEquals("LIKE", like.getValue());
        Assert.assertEquals("a", like.getChildren().get(0).getValue());
        Expression and = where.getChildren().get(1);
        Assert.assertEquals("AND", and.getValue());
        Expression not = and.getChildren().get(0);
        Assert.assertEquals("NOT", not.getValue());
        Assert.assertEquals("IN", not.getChildren().get(0).getValue());
        Assert.assertEquals(3, not.getChildren().get(0).getChildren().size());
        Assert.assertEquals("IS NOT NULL", and.getChildren().get(1).getValue());
    }
}
