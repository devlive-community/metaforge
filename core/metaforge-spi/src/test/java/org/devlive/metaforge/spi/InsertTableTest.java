package org.devlive.metaforge.spi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.devlive.metaforge.spi.generator.table.InsertTable;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class InsertTableTest
{
    private final ObjectMapper mapper = new ObjectMapper();

    private String insert(String row)
            throws Exception
    {
        return InsertTable.create("app", "flows")
                .addValues(Collections.singletonList(mapper.readTree(row)))
                .addPrimaryKeys(Collections.singletonList("id"))
                .build();
    }

    @Test
    public void enteredPrimaryKeysAreKept()
            throws Exception
    {
        Assert.assertEquals("INSERT INTO `app`.`flows` (`id`)\nVALUES (1011);", insert("{\"id\":1011}"));
        Assert.assertEquals("INSERT INTO `app`.`flows` (`id`)\nVALUES ('flow_a');", insert("{\"id\":\"flow_a\"}"));
    }

    @Test
    public void emptyPrimaryKeysLetTheDatabaseGenerateThem()
            throws Exception
    {
        Assert.assertEquals("INSERT INTO `app`.`flows` (`id`)\nVALUES (NULL);", insert("{\"id\":\"\"}"));
        Assert.assertEquals("INSERT INTO `app`.`flows` (`id`)\nVALUES (NULL);", insert("{\"id\":null}"));
    }
}
