package org.devlive.metaforge.condor.manager;

import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.condor.ComparisonOperator;
import org.devlive.metaforge.condor.DataType;
import org.devlive.metaforge.condor.DatabaseException;
import org.devlive.metaforge.condor.TableException;
import org.devlive.metaforge.condor.condition.SimpleCondition;
import org.devlive.metaforge.condor.metadata.ColumnDefinition;
import org.devlive.metaforge.condor.metadata.RowDefinition;
import org.devlive.metaforge.condor.metadata.TableDefinition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Slf4j
public class TableManagerTest
{
    private DatabaseManager databaseManager;
    private TableManager tableManager;

    @Before
    public void setUp()
            throws DatabaseException
    {
        databaseManager = DatabaseManager.createManager();
        if (!databaseManager.databaseExists("table_test_db")) {
            databaseManager.createDatabase("table_test_db");
        }
        databaseManager.useDatabase("table_test_db");
        tableManager = databaseManager.getCurrentDatabase().getTableManager();
    }

    @After
    public void tearDown()
    {
        try {
            databaseManager.dropDatabase("table_test_db");
        }
        catch (DatabaseException ignored) {
        }
    }

    private TableDefinition createUserTable()
    {
        List<ColumnDefinition> columns = Arrays.asList(
                new ColumnDefinition("id", DataType.INTEGER, true, false),
                new ColumnDefinition("name", DataType.VARCHAR, false, true),
                new ColumnDefinition("age", DataType.INTEGER, false, true)
        );
        return new TableDefinition("users", columns);
    }

    @Test
    public void testCreateTable()
            throws TableException
    {
        tableManager.createTable(createUserTable());
        assertTrue(tableManager.tableExists("users"));
    }

    @Test(expected = TableException.class)
    public void testCreateDuplicateTable()
            throws TableException
    {
        tableManager.createTable(createUserTable());
        tableManager.createTable(createUserTable());
    }

    @Test
    public void testDropTable()
            throws TableException
    {
        tableManager.createTable(createUserTable());
        assertTrue(tableManager.tableExists("users"));

        tableManager.dropTable("users");
        assertFalse(tableManager.tableExists("users"));
    }

    @Test(expected = TableException.class)
    public void testDropNonExistentTable()
            throws TableException
    {
        tableManager.dropTable("nonexistent");
    }

    @Test
    public void testInsertAndSelect()
            throws TableException
    {
        tableManager.createTable(createUserTable());

        tableManager.insert("users",
                Arrays.asList("id", "name", "age"),
                Arrays.asList(1, "Alice", 25));

        List<RowDefinition> rows = tableManager.select("users", null, null);
        assertEquals(1, rows.size());
        assertEquals(1, rows.get(0).getValue("id"));
        assertEquals("Alice", rows.get(0).getValue("name"));
        assertEquals(25, rows.get(0).getValue("age"));
    }

    @Test
    public void testBatchInsert()
            throws TableException
    {
        tableManager.createTable(createUserTable());

        List<List<Object>> valuesList = Arrays.asList(
                Arrays.asList(1, "Alice", 25),
                Arrays.asList(2, "Bob", 30),
                Arrays.asList(3, "Charlie", 35)
        );

        tableManager.batchInsert("users",
                Arrays.asList("id", "name", "age"),
                valuesList);

        List<RowDefinition> rows = tableManager.select("users", null, null);
        assertEquals(3, rows.size());
    }

    @Test
    public void testSelectWithColumnProjection()
            throws TableException
    {
        tableManager.createTable(createUserTable());

        tableManager.insert("users",
                Arrays.asList("id", "name", "age"),
                Arrays.asList(1, "Alice", 25));

        List<RowDefinition> rows = tableManager.select("users",
                Arrays.asList("name"), null);
        assertEquals(1, rows.size());
        assertEquals("Alice", rows.get(0).getValue("name"));
    }

    @Test
    public void testSelectWithWhereCondition()
            throws TableException
    {
        tableManager.createTable(createUserTable());

        tableManager.insert("users", Arrays.asList("id", "name", "age"), Arrays.asList(1, "Alice", 25));
        tableManager.insert("users", Arrays.asList("id", "name", "age"), Arrays.asList(2, "Bob", 30));
        tableManager.insert("users", Arrays.asList("id", "name", "age"), Arrays.asList(3, "Charlie", 35));

        SimpleCondition condition = new SimpleCondition("age", 30, ComparisonOperator.GREATER_THAN);
        List<RowDefinition> rows = tableManager.select("users", null, condition);
        assertEquals(1, rows.size());
        assertEquals("Charlie", rows.get(0).getValue("name"));
    }

    @Test
    public void testUpdate()
            throws TableException
    {
        tableManager.createTable(createUserTable());

        tableManager.insert("users", Arrays.asList("id", "name", "age"), Arrays.asList(1, "Alice", 25));
        tableManager.insert("users", Arrays.asList("id", "name", "age"), Arrays.asList(2, "Bob", 30));

        Map<String, Object> setValues = new HashMap<>();
        setValues.put("age", 26);

        SimpleCondition condition = new SimpleCondition("name", "Alice", ComparisonOperator.EQUALS);
        int updated = tableManager.update("users", setValues, condition);
        assertEquals(1, updated);

        List<RowDefinition> rows = tableManager.select("users", null,
                new SimpleCondition("name", "Alice", ComparisonOperator.EQUALS));
        assertEquals(26, rows.get(0).getValue("age"));
    }

    @Test
    public void testUpdateAll()
            throws TableException
    {
        tableManager.createTable(createUserTable());

        tableManager.insert("users", Arrays.asList("id", "name", "age"), Arrays.asList(1, "Alice", 25));
        tableManager.insert("users", Arrays.asList("id", "name", "age"), Arrays.asList(2, "Bob", 30));

        Map<String, Object> setValues = new HashMap<>();
        setValues.put("age", 0);

        int updated = tableManager.update("users", setValues, null);
        assertEquals(2, updated);

        List<RowDefinition> rows = tableManager.select("users", null, null);
        for (RowDefinition row : rows) {
            assertEquals(0, row.getValue("age"));
        }
    }

    @Test
    public void testDelete()
            throws TableException
    {
        tableManager.createTable(createUserTable());

        tableManager.insert("users", Arrays.asList("id", "name", "age"), Arrays.asList(1, "Alice", 25));
        tableManager.insert("users", Arrays.asList("id", "name", "age"), Arrays.asList(2, "Bob", 30));
        tableManager.insert("users", Arrays.asList("id", "name", "age"), Arrays.asList(3, "Charlie", 35));

        SimpleCondition condition = new SimpleCondition("id", 2, ComparisonOperator.EQUALS);
        int deleted = tableManager.delete("users", condition);
        assertEquals(1, deleted);

        List<RowDefinition> rows = tableManager.select("users", null, null);
        assertEquals(2, rows.size());
    }

    @Test
    public void testDeleteAll()
            throws TableException
    {
        tableManager.createTable(createUserTable());

        tableManager.insert("users", Arrays.asList("id", "name", "age"), Arrays.asList(1, "Alice", 25));
        tableManager.insert("users", Arrays.asList("id", "name", "age"), Arrays.asList(2, "Bob", 30));

        int deleted = tableManager.delete("users", null);
        assertEquals(2, deleted);

        List<RowDefinition> rows = tableManager.select("users", null, null);
        assertEquals(0, rows.size());
    }

    @Test
    public void testListTables()
            throws TableException
    {
        tableManager.createTable(createUserTable());

        List<ColumnDefinition> columns = Arrays.asList(
                new ColumnDefinition("oid", DataType.INTEGER, true, false),
                new ColumnDefinition("total", DataType.DOUBLE, false, true)
        );
        tableManager.createTable(new TableDefinition("orders", columns));

        String[] tables = tableManager.listTables();
        assertEquals(2, tables.length);
    }

    @Test(expected = TableException.class)
    public void testInsertInvalidColumn()
            throws TableException
    {
        tableManager.createTable(createUserTable());
        tableManager.insert("users",
                Arrays.asList("id", "nonexistent"),
                Arrays.asList(1, "value"));
    }

    @Test(expected = TableException.class)
    public void testInsertColumnCountMismatch()
            throws TableException
    {
        tableManager.createTable(createUserTable());
        tableManager.insert("users",
                Arrays.asList("id", "name"),
                Arrays.asList(1));
    }

    @Test(expected = TableException.class)
    public void testSelectNonExistentColumn()
            throws TableException
    {
        tableManager.createTable(createUserTable());
        tableManager.select("users", Arrays.asList("nonexistent"), null);
    }
}
