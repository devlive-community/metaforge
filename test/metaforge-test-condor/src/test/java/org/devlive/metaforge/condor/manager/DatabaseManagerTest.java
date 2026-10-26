package org.devlive.metaforge.condor.manager;

import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.condor.DatabaseException;
import org.devlive.metaforge.condor.metadata.DatabaseDefinition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Slf4j
public class DatabaseManagerTest
{
    private DatabaseManager manager;

    @Before
    public void setUp()
    {
        manager = DatabaseManager.createManager();
    }

    @After
    public void tearDown()
    {
        for (String db : manager.listDatabases()) {
            try {
                manager.dropDatabase(db);
            }
            catch (DatabaseException ignored) {
            }
        }
    }

    @Test
    public void testCreateDatabase()
            throws DatabaseException
    {
        manager.createDatabase("testdb");
        assertTrue(manager.databaseExists("testdb"));
    }

    @Test(expected = DatabaseException.class)
    public void testCreateDuplicateDatabase()
            throws DatabaseException
    {
        manager.createDatabase("dupdb");
        manager.createDatabase("dupdb");
    }

    @Test(expected = DatabaseException.class)
    public void testCreateDatabaseInvalidName()
            throws DatabaseException
    {
        manager.createDatabase("123invalid");
    }

    @Test(expected = DatabaseException.class)
    public void testCreateDatabaseEmptyName()
            throws DatabaseException
    {
        manager.createDatabase("");
    }

    @Test
    public void testDropDatabase()
            throws DatabaseException
    {
        manager.createDatabase("dropme");
        assertTrue(manager.databaseExists("dropme"));

        manager.dropDatabase("dropme");
        assertFalse(manager.databaseExists("dropme"));
    }

    @Test(expected = DatabaseException.class)
    public void testDropNonExistentDatabase()
            throws DatabaseException
    {
        manager.dropDatabase("nonexistent");
    }

    @Test
    public void testUseDatabase()
            throws DatabaseException
    {
        manager.createDatabase("usedb");
        manager.useDatabase("usedb");

        DatabaseDefinition current = manager.getCurrentDatabase();
        assertNotNull(current);
        assertEquals("usedb", current.getName());
    }

    @Test(expected = DatabaseException.class)
    public void testUseNonExistentDatabase()
            throws DatabaseException
    {
        manager.useDatabase("nonexistent");
    }

    @Test(expected = DatabaseException.class)
    public void testGetCurrentDatabaseWithoutSelection()
            throws DatabaseException
    {
        DatabaseManager freshManager = DatabaseManager.createManager();
        freshManager.getCurrentDatabase();
    }

    @Test
    public void testDropCurrentDatabaseResetsSelection()
            throws DatabaseException
    {
        manager.createDatabase("currentdb");
        manager.useDatabase("currentdb");
        manager.dropDatabase("currentdb");

        try {
            manager.getCurrentDatabase();
            fail("Should have thrown DatabaseException");
        }
        catch (DatabaseException e) {
            // expected
        }
    }

    @Test
    public void testListDatabases()
            throws DatabaseException
    {
        manager.createDatabase("db1");
        manager.createDatabase("db2");

        String[] databases = manager.listDatabases();
        assertEquals(2, databases.length);
    }
}
