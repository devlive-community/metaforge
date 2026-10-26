package org.devlive.metaforge.test.mongo;

import org.bson.Document;
import org.devlive.metaforge.driver.parser.MongoParser;
import org.devlive.metaforge.sql.SQLParseException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MongoShowParserTest
{
    @Test
    public void testShowDatabases()
    {
        // Basic SHOW DATABASES
        MongoParser parser = MongoParser.createParser("SHOW DATABASES");
        assertEquals("listDatabases", parser.getCommand());
        assertNull(parser.getFilter());

        // SHOW DATABASES with LIKE pattern
        parser = MongoParser.createParser("SHOW DATABASES LIKE '%test%'");
        assertEquals("listDatabases", parser.getCommand());
        assertNotNull(parser.getFilter());
        Document filter = parser.getFilter();
        assertTrue(filter.containsKey("name"));
        Document regex = (Document) filter.get("name");
        assertEquals(".*test.*", regex.get("$regex"));
    }

    @Test
    public void testShowTables()
    {
        // Basic SHOW TABLES
        MongoParser parser = MongoParser.createParser("SHOW TABLES");
        assertEquals("listCollections", parser.getCommand());
        assertNull(parser.getDatabase());
        assertNull(parser.getFilter());

        // SHOW TABLES FROM database
        parser = MongoParser.createParser("SHOW TABLES FROM mydb");
        assertEquals("listCollections", parser.getCommand());
        assertEquals("mydb", parser.getDatabase());
        assertNull(parser.getFilter());

        // SHOW TABLES with LIKE pattern
        parser = MongoParser.createParser("SHOW TABLES FROM mydb LIKE '%user%'");
        assertEquals("listCollections", parser.getCommand());
        assertEquals("mydb", parser.getDatabase());
        assertNotNull(parser.getFilter());
        Document filter = parser.getFilter();
        assertTrue(filter.containsKey("name"));
        Document regex = (Document) filter.get("name");
        assertEquals(".*user.*", regex.get("$regex"));
    }

    @Test
    public void testShowColumns()
    {
        // Basic SHOW COLUMNS
        MongoParser parser = MongoParser.createParser("SHOW COLUMNS FROM users");
        assertEquals("listFields", parser.getCommand());
        assertEquals("users", parser.getCollection());
        assertNull(parser.getDatabase());
        assertNull(parser.getFilter());

        // SHOW COLUMNS with database
        parser = MongoParser.createParser("SHOW COLUMNS FROM users FROM mydb");
        assertEquals("listFields", parser.getCommand());
        assertEquals("users", parser.getCollection());
        assertEquals("mydb", parser.getDatabase());
        assertNull(parser.getFilter());

        // SHOW COLUMNS with LIKE pattern
        parser = MongoParser.createParser("SHOW COLUMNS FROM users LIKE '%id%'");
        assertEquals("listFields", parser.getCommand());
        assertEquals("users", parser.getCollection());
        assertNotNull(parser.getFilter());
        Document filter = parser.getFilter();
        assertTrue(filter.containsKey("name"));
        Document regex = (Document) filter.get("name");
        assertEquals(".*id.*", regex.get("$regex"));
    }

    @Test(expected = SQLParseException.class)
    public void testInvalidStatement()
    {
        MongoParser.createParser("SHO");
    }
}
