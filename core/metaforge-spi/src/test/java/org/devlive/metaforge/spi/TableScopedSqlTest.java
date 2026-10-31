package org.devlive.metaforge.spi;

import org.devlive.metaforge.spi.sql.TableScopedSql;
import org.junit.Assert;
import org.junit.Test;

public class TableScopedSqlTest
{
    private static void allowed(String sql)
    {
        Assert.assertEquals(sql, java.util.Optional.empty(), TableScopedSql.validate(sql, "app", "orders"));
    }

    private static void rejected(String sql)
    {
        Assert.assertTrue(sql, TableScopedSql.validate(sql, "app", "orders").isPresent());
    }

    @Test
    public void statementsOnTheTableAreAllowed()
    {
        allowed("SELECT * FROM orders");
        allowed("select o.id, count(*) from `app`.`orders` as o where o.state = 'x; FROM other' group by o.id order by 1 limit 10;");
        allowed("SELECT * FROM orders a JOIN orders b ON a.parent = b.id");
        allowed("SELECT * FROM orders WHERE id IN (SELECT parent FROM ORDERS WHERE x = 1) -- FROM other");
        allowed("SELECT * FROM (SELECT id FROM orders) t");
        allowed("INSERT INTO orders (id) VALUES (1) ON DUPLICATE KEY UPDATE id = 2");
        allowed("INSERT INTO app.orders SELECT * FROM orders WHERE id = 1");
        allowed("UPDATE orders SET state = 'from users' WHERE id = 1");
        allowed("DELETE FROM orders WHERE id = 1");
        allowed("SELECT * FROM orders FOR UPDATE");
        allowed("SELECT * FROM orders o, orders p WHERE o.id = p.id");
    }

    @Test
    public void otherTablesAndStatementsAreRejected()
    {
        rejected("SELECT * FROM users");
        rejected("SELECT * FROM other.orders");
        rejected("SELECT * FROM orders JOIN users ON 1 = 1");
        rejected("SELECT * FROM orders, users");
        rejected("SELECT * FROM orders WHERE id IN (SELECT id FROM users)");
        rejected("SELECT * FROM orders UNION SELECT * FROM users");
        rejected("SELECT * FROM information_schema.TABLES");
        rejected("SELECT 1");
        rejected("SELECT * FROM orders; DROP TABLE orders");
        rejected("DROP TABLE orders");
        rejected("SHOW TABLES");
        rejected("WITH x AS (SELECT 1) SELECT * FROM orders");
        rejected("SELECT * FROM orders INTO OUTFILE '/tmp/x'");
        rejected("INSERT INTO orders SELECT * FROM users");
        rejected("UPDATE orders, users SET orders.id = users.id");
        rejected("SELECT LOAD_FILE('/etc/passwd') FROM orders");
        rejected("SELECT * FROM JSON_TABLE('[]', '$[*]' COLUMNS (a INT PATH '$')) t");
        rejected("");
    }
}
