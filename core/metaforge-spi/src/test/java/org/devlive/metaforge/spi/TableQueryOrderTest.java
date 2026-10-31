package org.devlive.metaforge.spi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.devlive.metaforge.spi.generator.definition.ColumnDefinition;
import org.devlive.metaforge.spi.generator.definition.TableDefinition;
import org.junit.Assert;
import org.junit.Test;

import java.util.stream.Collectors;

public class TableQueryOrderTest
{
    @Test
    public void sortRulesKeepTheirRequestOrder()
            throws Exception
    {
        String body = "{\"orders\":[{\"name\":\"created_at\",\"order\":\"desc\"},{\"name\":\"id\",\"order\":\"asc\"},"
                + "{\"name\":\"name\",\"order\":\"asc\"},{\"name\":\"code\",\"order\":\"desc\"},{\"name\":\"type\",\"order\":\"asc\"}]}";
        TableDefinition definition = new ObjectMapper().readValue(body, TableDefinition.class);
        Assert.assertEquals("created_at,id,name,code,type",
                definition.getOrders().stream().map(ColumnDefinition::getName).collect(Collectors.joining(",")));
    }
}
