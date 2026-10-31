package org.devlive.metaforge.spi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.devlive.metaforge.spi.generator.definition.TableDefinition;
import org.devlive.metaforge.spi.generator.definition.TriggerDefinition;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TriggerStatementTest
{
    private final List<String> executed = Lists.newArrayList();

    /** Fails CREATE statements whose body contains "broken"; answers SHOW CREATE with the original. */
    private final PluginService service = new PluginService()
    {
        @Override
        public Response execute(RuntimeConfigure configure, String content)
        {
            executed.add(content);
            if (content.startsWith("SHOW CREATE TRIGGER")) {
                // ObjectNode is Iterable, so Lists.newArrayList(node) would spread its values instead.
                List<Object> rows = new ArrayList<>();
                rows.add(new ObjectMapper().createObjectNode().put("SQL Original Statement", "CREATE TRIGGER `trg` BEFORE INSERT ON `user` FOR EACH ROW SET NEW.a = 1"));
                return Response.builder().isSuccessful(true).columns(rows).build();
            }
            boolean failed = content.contains("broken");
            return Response.builder().isSuccessful(!failed).message(failed ? "syntax error" : null).columns(Lists.newArrayList()).build();
        }
    };

    private static TableDefinition definition(String body, boolean preview)
    {
        TriggerDefinition trigger = new TriggerDefinition();
        trigger.setName("trg");
        trigger.setTiming("before");
        trigger.setEvent("insert");
        trigger.setDefiner("root@%");
        trigger.setBody(body);
        TableDefinition definition = TableDefinition.create("app", "user");
        definition.setPreview(preview);
        definition.setTriggers(Lists.newArrayList(trigger));
        return definition;
    }

    @Test
    public void createBuildsTheStatementWithDefinerAndBody()
    {
        Assert.assertEquals("CREATE DEFINER = 'root'@'%' TRIGGER `app`.`trg`\nBEFORE INSERT ON `app`.`user`\nFOR EACH ROW\nBEGIN\n  SET NEW.a = 1;\nEND;",
                service.createTrigger(new RuntimeConfigure(), definition("BEGIN\n  SET NEW.a = 1;\nEND;;", true)).getContent());
    }

    @Test
    public void aFailedEditRestoresTheOriginalTrigger()
    {
        Response response = service.alterTrigger(new RuntimeConfigure(), definition("broken", false));
        Assert.assertFalse(response.getIsSuccessful());
        Assert.assertTrue(response.getMessage(), response.getMessage().contains("the original trigger was restored"));
        Assert.assertEquals("CREATE TRIGGER `trg` BEFORE INSERT ON `user` FOR EACH ROW SET NEW.a = 1", executed.get(executed.size() - 1));
        Assert.assertTrue(executed.get(1).startsWith("DROP TRIGGER `app`.`trg`"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void eventsAreLimitedToRowChanges()
    {
        TableDefinition definition = definition("SET NEW.a = 1", true);
        definition.getTriggers().get(0).setEvent("TRUNCATE");
        service.createTrigger(new RuntimeConfigure(), definition);
    }
}
