package org.devlive.metaforge.test.mongo;

import org.devlive.metaforge.driver.parser.MongoParser;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MongoSelectParserTest
{
    private final static String sql = "SELECT name, age FROM users WHERE age > 18 AND city = 'Beijing' ORDER BY age DESC LIMIT 10";
    private MongoParser parser;

    @Before
    public void before()
    {
        this.parser = MongoParser.createParser(sql);
    }

    @Test
    public void testSelect()
    {
        assertEquals("users", parser.getCollection());
        assertEquals("{\"aggregate\": \"users\", \"pipeline\": [{\"$match\": {\"$and\": [{\"age\": {\"$gt\": 18}}, {\"city\": \"Beijing\"}]}}, {\"$project\": {\"_id\": 0, \"name\": 1, \"age\": 1}}, {\"$sort\": {\"age\": -1}}, {\"$limit\": 10}], \"cursor\": {}}", parser.getQuery().toJson());
    }
}
