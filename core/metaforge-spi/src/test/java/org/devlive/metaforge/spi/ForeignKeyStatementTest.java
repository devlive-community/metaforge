package org.devlive.metaforge.spi;

import com.google.common.collect.Lists;
import org.devlive.metaforge.spi.generator.definition.ForeignKeyDefinition;
import org.devlive.metaforge.spi.generator.definition.TableDefinition;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.junit.Assert;
import org.junit.Test;

public class ForeignKeyStatementTest
{
    private final PluginService service = new PluginService() {};

    private static TableDefinition definition(ForeignKeyDefinition key)
    {
        TableDefinition definition = TableDefinition.create("app", "user");
        definition.setPreview(true);
        definition.setForeignKeys(Lists.newArrayList(key));
        return definition;
    }

    private static ForeignKeyDefinition key()
    {
        ForeignKeyDefinition key = new ForeignKeyDefinition();
        key.setName("fk_user_role");
        key.setColumns(Lists.newArrayList("role_id"));
        key.setReferencedTable("role");
        key.setReferencedColumns(Lists.newArrayList("id"));
        key.setOnUpdate("cascade");
        key.setOnDelete("set  null");
        return key;
    }

    @Test
    public void createAddsTheConstraintAsTheProtoypeShows()
    {
        Response response = service.createForeignKey(new RuntimeConfigure(), definition(key()));
        Assert.assertEquals("ALTER TABLE `app`.`user`\n    ADD CONSTRAINT `fk_user_role`\n    FOREIGN KEY (`role_id`)\n    REFERENCES `app`.`role` (`id`)"
                + "\n    ON UPDATE CASCADE\n    ON DELETE SET NULL;", response.getContent());
    }

    @Test
    public void editsDropFirstAndUnknownRulesAreLeftOut()
    {
        ForeignKeyDefinition key = key();
        key.setOriginalName("fk_old");
        key.setReferencedDatabase("other");
        key.setOnUpdate("SET DEFAULT");
        key.setOnDelete(null);
        Response response = service.alterForeignKey(new RuntimeConfigure(), definition(key));
        Assert.assertEquals("ALTER TABLE `app`.`user` DROP FOREIGN KEY `fk_old`;\nALTER TABLE `app`.`user`\n    ADD CONSTRAINT `fk_user_role`\n"
                + "    FOREIGN KEY (`role_id`)\n    REFERENCES `other`.`role` (`id`);", response.getContent());
        Assert.assertEquals("ALTER TABLE `app`.`user` DROP FOREIGN KEY `fk_user_role`;", service.dropForeignKey(new RuntimeConfigure(), definition(key())).getContent());
    }
}
