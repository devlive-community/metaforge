package org.devlive.metaforge.spi;

import org.devlive.metaforge.spi.generator.definition.TableOptionsDefinition;
import org.junit.Assert;
import org.junit.Test;

public class TableOptionsStatementTest
{
    private final PluginService service = new PluginService() {};

    @Test
    public void onlyTheGivenOptionsAreChanged()
    {
        TableOptionsDefinition options = new TableOptionsDefinition();
        options.setEngine("InnoDB");
        options.setRowFormat("dynamic");
        options.setAutoIncrement(16L);
        options.setCharset("utf8mb4");
        options.setCollate("utf8mb4_0900_ai_ci");
        options.setComment("工作流定义表，it's");
        Assert.assertEquals("ALTER TABLE `app`.`workflow`\n    ENGINE = InnoDB\n    ROW_FORMAT = DYNAMIC\n    AUTO_INCREMENT = 16\n"
                + "    DEFAULT CHARACTER SET = utf8mb4\n    COLLATE = utf8mb4_0900_ai_ci\n    COMMENT = '工作流定义表，it''s'", service.tableOptionsStatement("app", "workflow", options));
        TableOptionsDefinition comment = new TableOptionsDefinition();
        comment.setComment("");
        Assert.assertEquals("ALTER TABLE `app`.`workflow`\n    COMMENT = ''", service.tableOptionsStatement("app", "workflow", comment));
    }

    @Test(expected = IllegalArgumentException.class)
    public void wordsWithSyntaxAreRefused()
    {
        TableOptionsDefinition options = new TableOptionsDefinition();
        options.setEngine("InnoDB; DROP TABLE x");
        service.tableOptionsStatement("app", "workflow", options);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nothingToChangeIsRefused()
    {
        service.tableOptionsStatement("app", "workflow", new TableOptionsDefinition());
    }
}
