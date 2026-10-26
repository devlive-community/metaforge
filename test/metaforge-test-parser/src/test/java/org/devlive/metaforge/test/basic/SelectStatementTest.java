package org.devlive.metaforge.test.basic;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.sql.SQLParseException;
import org.devlive.metaforge.sql.SQLParser;
import org.devlive.metaforge.sql.node.Expression;
import org.devlive.metaforge.sql.node.clause.JoinClause;
import org.devlive.metaforge.sql.node.element.OrderByElement;
import org.devlive.metaforge.sql.node.element.SelectElement;
import org.devlive.metaforge.sql.node.element.TableElement;
import org.devlive.metaforge.sql.statement.SQLStatement;
import org.devlive.metaforge.sql.statement.SelectStatement;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@SuppressFBWarnings(value = {"JUA_DONT_ASSERT_INSTANCEOF_IN_TESTS"})
public class SelectStatementTest
{
    @Test
    public void testSimpleSelect()
    {
        String sql = "SELECT id, name FROM users";
        SQLStatement stmt = SQLParser.parse(sql);

        assertTrue(stmt instanceof SelectStatement);
        SelectStatement select = (SelectStatement) stmt;

        List<SelectElement> selectElements = select.getSelectElements();
        assertEquals(2, selectElements.size());
        assertEquals("id", selectElements.get(0).getColumn());
        assertEquals("name", selectElements.get(1).getColumn());

        List<TableElement> fromSources = select.getFromSources();
        assertEquals(1, fromSources.size());
        assertEquals("users", fromSources.get(0).getTableName());
    }

    @Test
    public void testSelectWithWhereAndOrderBy()
    {
        String sql = "SELECT id, name FROM users WHERE age > 18 ORDER BY name DESC";
        SelectStatement select = (SelectStatement) SQLParser.parse(sql);

        Expression where = select.getWhereClause();
        assertNotNull(where);
        assertEquals(Expression.ExpressionType.BINARY_OP, where.getType());

        List<OrderByElement> orderBy = select.getOrderByElements();
        assertEquals(1, orderBy.size());
        assertFalse(orderBy.get(0).isAscending());
    }

    @Test
    public void testSelectWithJoin()
    {
        String sql = "SELECT u.id, u.name, o.order_id " +
                "FROM users u " +
                "LEFT JOIN orders o ON u.id = o.user_id";
        SelectStatement select = (SelectStatement) SQLParser.parse(sql);

        List<TableElement> fromSources = select.getFromSources();
        assertEquals(1, fromSources.size());
        TableElement mainTable = fromSources.get(0);
        assertEquals("users", mainTable.getTableName());
        assertEquals("u", mainTable.getAlias());

        List<JoinClause> joins = mainTable.getJoins();
        assertEquals(1, joins.size());
        JoinClause join = joins.get(0);
        assertEquals(JoinClause.JoinType.LEFT, join.getJoinType());
        assertEquals("orders", join.getRightTable().getTableName());
        assertEquals("o", join.getRightTable().getAlias());
    }

    @Test
    public void testComplexSelect()
    {
        String sql = "SELECT " +
                "   u.id, " +
                "   u.name, " +
                "   COUNT(o.order_id) as order_count " +
                "FROM users u " +
                "LEFT JOIN orders o ON u.id = o.user_id " +
                "WHERE u.status = 'active' " +
                "GROUP BY u.id, u.name " +
                "HAVING COUNT(o.order_id) > 5 " +
                "ORDER BY order_count DESC " +
                "LIMIT 10";

        SelectStatement select = (SelectStatement) SQLParser.parse(sql);

        List<SelectElement> selectElements = select.getSelectElements();
        assertEquals(3, selectElements.size());
        assertEquals("order_count", selectElements.get(2).getAlias());

        List<Expression> groupBy = select.getGroupByElements();
        assertEquals(2, groupBy.size());

        Expression having = select.getHavingClause();
        assertNotNull(having);

        assertNotNull(select.getLimitClause());
        assertEquals(10, select.getLimitClause().getLimit());
    }

    @Test
    public void testSelectWithSubquery()
    {
        String sql = "SELECT * FROM (SELECT id, name FROM users) as u";
        SelectStatement select = (SelectStatement) SQLParser.parse(sql);

        List<TableElement> fromSources = select.getFromSources();
        assertNotNull("FromSources should not be null", fromSources);
        assertEquals("Should have one source", 1, fromSources.size());

        TableElement table = fromSources.get(0);
        assertNotNull("Table should not be null", table);
        assertEquals("Alias should be 'u'", "u", table.getAlias());

        SelectStatement subquery = table.getSubquery();
        assertNotNull("Subquery should not be null", subquery);

        List<SelectElement> subqueryElements = subquery.getSelectElements();
        assertEquals("Subquery should have 2 columns", 2, subqueryElements.size());
        assertEquals("First column should be 'id'", "id", subqueryElements.get(0).getColumn());
        assertEquals("Second column should be 'name'", "name", subqueryElements.get(1).getColumn());
    }

    @Test
    public void testSelectWithCaseWhen()
    {
        String sql = "SELECT id, " +
                "CASE WHEN age < 18 THEN 'minor' " +
                "     WHEN age < 60 THEN 'adult' " +
                "     ELSE 'senior' END as age_group " +
                "FROM users";

        SelectStatement select = (SelectStatement) SQLParser.parse(sql);
        List<SelectElement> selectElements = select.getSelectElements();
        assertEquals(2, selectElements.size());
        assertEquals("age_group", selectElements.get(1).getAlias());
    }

    @Test(expected = SQLParseException.class)
    public void testInvalidSQL()
    {
        String sql = "SELECT * FORM users";
        SQLParser.parse(sql);
    }
}
