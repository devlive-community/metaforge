package org.devlive.metaforge.spi;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.devlive.metaforge.spi.generator.definition.IndexDefinition;
import org.devlive.metaforge.spi.generator.definition.TableDefinition;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.junit.Assert;
import org.junit.Test;

public class IndexStatementTest
{
    private final PluginService service = new PluginService() {};

    private static IndexDefinition index(String name, String type, boolean unique, String... columns)
    {
        IndexDefinition index = new IndexDefinition();
        index.setName(name);
        index.setType(type);
        index.setUnique(unique);
        index.setColumns(Lists.newArrayList(columns));
        return index;
    }

    @Test
    public void createBuildsTheIndexAsTheProtoypeShows()
    {
        IndexDefinition index = index("idx_status_created", "BTREE", false, "status", "created_at");
        index.setOrders(Lists.newArrayList("ASC", "DESC"));
        Assert.assertEquals("CREATE INDEX `idx_status_created` USING BTREE\n    ON `app`.`user` (`status` ASC, `created_at` DESC)",
                service.createIndexStatement("app", "user", index));
        index.setComment("it's composite");
        index.setAlgorithm("inplace");
        index.setUnique(true);
        index.setLengths(Lists.newArrayList(null, 10));
        Assert.assertEquals("CREATE UNIQUE INDEX `idx_status_created` USING BTREE\n    ON `app`.`user` (`status` ASC, `created_at`(10) DESC) COMMENT 'it''s composite'\n    ALGORITHM = INPLACE",
                service.createIndexStatement("app", "user", index));
    }

    @Test
    public void fulltextAndPrimaryUseTheirOwnSyntax()
    {
        IndexDefinition fulltext = index("ft_body", "FULLTEXT", true, "body");
        fulltext.setOrders(Lists.newArrayList("DESC"));
        Assert.assertEquals("CREATE FULLTEXT INDEX `ft_body`\n    ON `app`.`posts` (`body`)", service.createIndexStatement("app", "posts", fulltext));
        Assert.assertEquals("ALTER TABLE `app`.`posts`\n    ADD PRIMARY KEY USING BTREE (`id`)", service.createIndexStatement("app", "posts", index("PRIMARY", "BTREE", true, "id")));
    }

    @Test
    public void editsAndRebuildsDropAndAddInOneStatement()
    {
        IndexDefinition index = index("idx_new", "BTREE", false, "status");
        index.setOriginalName("idx_old");
        index.setAlgorithm("COPY");
        Assert.assertEquals("ALTER TABLE `app`.`user`\n    DROP INDEX `idx_old`,\n    ADD INDEX `idx_new` USING BTREE (`status`),\n    ALGORITHM = COPY",
                service.alterIndexStatement("app", "user", index));
        Assert.assertEquals("ALTER TABLE `app`.`user`\n    DROP PRIMARY KEY,\n    ADD PRIMARY KEY (`id`)", service.alterIndexStatement("app", "user", index("PRIMARY", null, true, "id")));
    }

    @Test
    public void dropPreviewsWithoutExecuting()
    {
        TableDefinition definition = TableDefinition.create("app", "user");
        definition.setPreview(true);
        definition.setIndexes(Sets.newLinkedHashSet(Lists.newArrayList(index("idx_a", null, false, "a"), index("PRIMARY", null, true, "id"))));
        Response response = service.dropIndex(new RuntimeConfigure(), definition);
        Assert.assertEquals("DROP INDEX `idx_a` ON `app`.`user`;\nALTER TABLE `app`.`user` DROP PRIMARY KEY;", response.getContent());
    }
}
