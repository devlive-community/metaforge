package org.devlive.metaforge.condor;

import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.condor.manager.DatabaseManager;
import org.devlive.metaforge.condor.metadata.RowDefinition;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Slf4j
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SQLExecutorTest
{
    private DatabaseManager databaseManager;
    private SQLExecutor executor;

    @Before
    public void setUp()
    {
        databaseManager = DatabaseManager.createManager();
        executor = new SQLExecutor(databaseManager);

        executor.execute("CREATE DATABASE IF NOT EXISTS executor_test");
        executor.execute("USE executor_test");
        executor.execute("DROP TABLE IF EXISTS users");
        executor.execute("CREATE TABLE IF NOT EXISTS users (id INT, name VARCHAR(255), age INT)");
    }

    @After
    public void tearDown()
    {
        executor.execute("DROP DATABASE IF EXISTS executor_test");
    }

    @Test
    public void testCreateDatabase()
    {
        SQLResult<Void> result = executor.execute("CREATE DATABASE IF NOT EXISTS newdb");
        assertTrue(result.isSuccess());
        executor.execute("DROP DATABASE IF EXISTS newdb");
    }

    @Test
    public void testDropDatabase()
    {
        executor.execute("CREATE DATABASE IF NOT EXISTS dropdb");
        SQLResult<Void> result = executor.execute("DROP DATABASE IF EXISTS dropdb");
        assertTrue(result.isSuccess());
    }

    @Test
    public void testUseDatabase()
    {
        SQLResult<Void> result = executor.execute("USE executor_test");
        assertTrue(result.isSuccess());
    }

    @Test
    public void testCreateTable()
    {
        executor.execute("DROP TABLE IF EXISTS new_table");
        SQLResult<Void> result = executor.execute(
                "CREATE TABLE IF NOT EXISTS new_table (id INT, value VARCHAR(100))");
        assertTrue(result.isSuccess());
    }

    @Test
    public void testCreateTableIfNotExists()
    {
        SQLResult<Void> result = executor.execute(
                "CREATE TABLE IF NOT EXISTS users (id INT, name VARCHAR(255), age INT)");
        assertTrue(result.isSuccess());
    }

    @Test
    public void testDropTable()
    {
        executor.execute("CREATE TABLE IF NOT EXISTS droptable (id INT)");
        SQLResult<Void> result = executor.execute("DROP TABLE IF EXISTS droptable");
        assertTrue(result.isSuccess());
    }

    @Test
    public void testDropTableIfExists()
    {
        SQLResult<Void> result = executor.execute("DROP TABLE IF EXISTS nonexistent_table");
        assertTrue(result.isSuccess());
    }

    @Test
    public void testInsertSingle()
    {
        SQLResult<Integer> result = executor.execute(
                "INSERT INTO users (id, name, age) VALUES (1, 'Alice', 25)");
        assertTrue(result.isSuccess());
    }

    @Test
    public void testInsertBatch()
    {
        SQLResult<Integer> result = executor.execute(
                "INSERT INTO users (id, name, age) VALUES (1, 'Alice', 25), (2, 'Bob', 30)");
        assertTrue(result.isSuccess());
    }

    @Test
    public void testSelectAll()
    {
        executor.execute("INSERT INTO users (id, name, age) VALUES (1, 'Alice', 25)");
        executor.execute("INSERT INTO users (id, name, age) VALUES (2, 'Bob', 30)");

        SQLResult<List<RowDefinition>> result = executor.execute(
                "SELECT id, name, age FROM users");
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(2, result.getData().size());
    }

    @Test
    public void testSelectWithWhere()
    {
        executor.execute("INSERT INTO users (id, name, age) VALUES (1, 'Alice', 25)");
        executor.execute("INSERT INTO users (id, name, age) VALUES (2, 'Bob', 30)");
        executor.execute("INSERT INTO users (id, name, age) VALUES (3, 'Charlie', 35)");

        SQLResult<List<RowDefinition>> result = executor.execute(
                "SELECT id, name, age FROM users WHERE age > 28");
        assertTrue(result.isSuccess());
        assertEquals(2, result.getData().size());
    }

    @Test
    public void testSelectWithWhereEquals()
    {
        executor.execute("INSERT INTO users (id, name, age) VALUES (1, 'Alice', 25)");
        executor.execute("INSERT INTO users (id, name, age) VALUES (2, 'Bob', 30)");

        SQLResult<List<RowDefinition>> result = executor.execute(
                "SELECT id, name FROM users WHERE name = 'Bob'");
        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        assertEquals("Bob", result.getData().get(0).getValue("name"));
    }

    @Test
    public void testSelectWithWhereAnd()
    {
        executor.execute("INSERT INTO users (id, name, age) VALUES (1, 'Alice', 25)");
        executor.execute("INSERT INTO users (id, name, age) VALUES (2, 'Bob', 30)");
        executor.execute("INSERT INTO users (id, name, age) VALUES (3, 'Charlie', 35)");

        SQLResult<List<RowDefinition>> result = executor.execute(
                "SELECT id, name FROM users WHERE age > 20 AND age < 32");
        assertTrue(result.isSuccess());
        assertEquals(2, result.getData().size());
    }

    @Test
    public void testSelectWithWhereOr()
    {
        executor.execute("INSERT INTO users (id, name, age) VALUES (1, 'Alice', 25)");
        executor.execute("INSERT INTO users (id, name, age) VALUES (2, 'Bob', 30)");
        executor.execute("INSERT INTO users (id, name, age) VALUES (3, 'Charlie', 35)");

        SQLResult<List<RowDefinition>> result = executor.execute(
                "SELECT id, name FROM users WHERE name = 'Alice' OR name = 'Charlie'");
        assertTrue(result.isSuccess());
        assertEquals(2, result.getData().size());
    }

    @Test
    public void testUpdate()
    {
        executor.execute("INSERT INTO users (id, name, age) VALUES (1, 'Alice', 25)");
        executor.execute("INSERT INTO users (id, name, age) VALUES (2, 'Bob', 30)");

        SQLResult<Integer> result = executor.execute(
                "UPDATE users SET age = 26 WHERE name = 'Alice'");
        assertTrue(result.isSuccess());

        SQLResult<List<RowDefinition>> selectResult = executor.execute(
                "SELECT id, name, age FROM users WHERE name = 'Alice'");
        assertEquals(26, selectResult.getData().get(0).getValue("age"));
    }

    @Test
    public void testUpdateMultipleColumns()
    {
        executor.execute("INSERT INTO users (id, name, age) VALUES (1, 'Alice', 25)");

        SQLResult<Integer> result = executor.execute(
                "UPDATE users SET name = 'Alicia', age = 26 WHERE id = 1");
        assertTrue(result.isSuccess());

        SQLResult<List<RowDefinition>> selectResult = executor.execute(
                "SELECT id, name, age FROM users WHERE id = 1");
        assertEquals("Alicia", selectResult.getData().get(0).getValue("name"));
        assertEquals(26, selectResult.getData().get(0).getValue("age"));
    }

    @Test
    public void testDelete()
    {
        executor.execute("INSERT INTO users (id, name, age) VALUES (1, 'Alice', 25)");
        executor.execute("INSERT INTO users (id, name, age) VALUES (2, 'Bob', 30)");
        executor.execute("INSERT INTO users (id, name, age) VALUES (3, 'Charlie', 35)");

        SQLResult<Integer> result = executor.execute(
                "DELETE FROM users WHERE id = 2");
        assertTrue(result.isSuccess());

        SQLResult<List<RowDefinition>> selectResult = executor.execute(
                "SELECT id, name FROM users");
        assertEquals(2, selectResult.getData().size());
    }

    @Test
    public void testDeleteWithCondition()
    {
        executor.execute("INSERT INTO users (id, name, age) VALUES (1, 'Alice', 25)");
        executor.execute("INSERT INTO users (id, name, age) VALUES (2, 'Bob', 30)");
        executor.execute("INSERT INTO users (id, name, age) VALUES (3, 'Charlie', 35)");

        SQLResult<Integer> result = executor.execute(
                "DELETE FROM users WHERE age >= 30");
        assertTrue(result.isSuccess());

        SQLResult<List<RowDefinition>> selectResult = executor.execute(
                "SELECT id, name FROM users");
        assertEquals(1, selectResult.getData().size());
        assertEquals("Alice", selectResult.getData().get(0).getValue("name"));
    }

    @Test
    public void testShowDatabases()
    {
        SQLResult<List<RowDefinition>> result = executor.execute("SHOW DATABASES");
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertFalse(result.getData().isEmpty());
    }

    @Test
    public void testShowTables()
    {
        SQLResult<List<RowDefinition>> result = executor.execute("SHOW TABLES");
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }

    @Test
    public void testShowColumns()
    {
        SQLResult<List<RowDefinition>> result = executor.execute(
                "SHOW COLUMNS FROM users");
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(3, result.getData().size());

        RowDefinition firstCol = result.getData().get(0);
        assertNotNull(firstCol.getValue("Field"));
        assertNotNull(firstCol.getValue("Type"));
    }

    @Test
    public void testFullLifecycle()
    {
        // Insert
        executor.execute("INSERT INTO users (id, name, age) VALUES (1, 'Alice', 25)");
        executor.execute("INSERT INTO users (id, name, age) VALUES (2, 'Bob', 30)");
        executor.execute("INSERT INTO users (id, name, age) VALUES (3, 'Charlie', 35)");

        // Select all
        SQLResult<List<RowDefinition>> all = executor.execute("SELECT id, name, age FROM users");
        assertEquals(3, all.getData().size());

        // Update
        executor.execute("UPDATE users SET age = 31 WHERE name = 'Bob'");

        // Verify update
        SQLResult<List<RowDefinition>> updated = executor.execute(
                "SELECT id, name, age FROM users WHERE name = 'Bob'");
        assertEquals(31, updated.getData().get(0).getValue("age"));

        // Delete
        executor.execute("DELETE FROM users WHERE age > 32");

        // Verify delete
        SQLResult<List<RowDefinition>> afterDelete = executor.execute("SELECT id, name, age FROM users");
        assertEquals(2, afterDelete.getData().size());
    }

    @Test
    public void testUnsupportedSQL()
    {
        SQLResult<?> result = executor.execute("THIS IS NOT SQL");
        assertFalse(result.isSuccess());
    }
}
