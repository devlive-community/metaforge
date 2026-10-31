package org.devlive.metaforge.spi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.devlive.metaforge.spi.sql.TableDdl;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TableDdlTest
{
    private static final String CREATE = "CREATE TABLE `user` (\n"
            + "  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',\n"
            + "  `name` varchar(50) NOT NULL COMMENT 'it''s, a name',\n"
            + "  `upper_name` varchar(50) GENERATED ALWAYS AS (upper(`name`)) VIRTUAL,\n"
            + "  `dept_id` bigint DEFAULT NULL,\n"
            + "  PRIMARY KEY (`id`),\n"
            + "  UNIQUE KEY `uk_name` (`name`),\n"
            + "  KEY `idx_dept` (`dept_id`),\n"
            + "  CONSTRAINT `fk_dept` FOREIGN KEY (`dept_id`) REFERENCES `dept` (`id`) ON DELETE CASCADE,\n"
            + "  CONSTRAINT `chk_id` CHECK ((`id` > 0))\n"
            + ") ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AUTO_INCREMENT=1 table'\n"
            + "/*!50100 PARTITION BY HASH (`id`)\nPARTITIONS 2 */";
    private static final String TARGET = "`app`.`user`";

    private final List<String> executed = Lists.newArrayList();

    /** Answers SHOW CREATE TABLE with the statement above and the sample query with two rows. */
    private final PluginService service = new PluginService()
    {
        @Override
        public Response execute(RuntimeConfigure configure, String content)
        {
            executed.add(content);
            ObjectMapper mapper = new ObjectMapper();
            List<Object> rows = new ArrayList<>();
            if (content.startsWith("SHOW CREATE TABLE")) {
                rows.add(mapper.createObjectNode().put("Table", "user").put("Create Table", CREATE));
            }
            else {
                ObjectNode first = mapper.createObjectNode().put("id", 1).put("name", "O'Neil");
                first.putNull("dept_id");
                rows.add(first);
                rows.add(mapper.createObjectNode().put("id", 2).put("name", "b").put("dept_id", 7));
            }
            return Response.builder().isSuccessful(true).columns(rows).build();
        }
    };

    @Test
    public void structureKeepsColumnsAndOptionsWithoutKeysCounterOrPartitions()
    {
        Assert.assertEquals("CREATE TABLE `user` (\n"
                + "  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',\n"
                + "  `name` varchar(50) NOT NULL COMMENT 'it''s, a name',\n"
                + "  `upper_name` varchar(50) GENERATED ALWAYS AS (upper(`name`)) VIRTUAL,\n"
                + "  `dept_id` bigint DEFAULT NULL\n"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AUTO_INCREMENT=1 table';", TableDdl.parse(CREATE).structure());
    }

    @Test
    public void indexesAndConstraintsBecomeAlterTableAdd()
    {
        TableDdl ddl = TableDdl.parse(CREATE);
        Assert.assertEquals("ALTER TABLE `app`.`user` ADD PRIMARY KEY (`id`);\n"
                + "ALTER TABLE `app`.`user` ADD UNIQUE KEY `uk_name` (`name`);\n"
                + "ALTER TABLE `app`.`user` ADD KEY `idx_dept` (`dept_id`);", ddl.indexes(TARGET));
        Assert.assertEquals("ALTER TABLE `app`.`user` ADD PRIMARY KEY (`id`);\n"
                + "ALTER TABLE `app`.`user` ADD UNIQUE KEY `uk_name` (`name`);\n"
                + "ALTER TABLE `app`.`user` ADD CONSTRAINT `fk_dept` FOREIGN KEY (`dept_id`) REFERENCES `dept` (`id`) ON DELETE CASCADE;\n"
                + "ALTER TABLE `app`.`user` ADD CONSTRAINT `chk_id` CHECK ((`id` > 0));", ddl.constraints(TARGET));
    }

    @Test
    public void alterStatementsRestateColumnsAndOptions()
    {
        TableDdl ddl = TableDdl.parse(CREATE);
        Assert.assertTrue(ddl.alterColumns(TARGET).startsWith("ALTER TABLE `app`.`user` MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键';\n"));
        Assert.assertEquals(4, ddl.alterColumns(TARGET).split("\n").length);
        Assert.assertTrue(ddl.alterTable(TARGET).endsWith(",\n  ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AUTO_INCREMENT=1 table';"));
    }

    @Test
    public void sampleDataSkipsGeneratedColumnsAndQuotesValues()
    {
        Response response = service.getTableDdl(new RuntimeConfigure(), "app", "user", "sample-data");
        Assert.assertEquals("SELECT `id`, `name`, `dept_id` FROM `app`.`user` LIMIT 10", executed.get(1));
        Assert.assertEquals("INSERT INTO `app`.`user` (`id`, `name`, `dept_id`) VALUES\n  (1, 'O''Neil', NULL),\n  (2, 'b', 7);", response.getContent());
    }

    @Test
    public void createIsPrintedAsIs()
    {
        Assert.assertEquals(CREATE + ";", service.getTableDdl(new RuntimeConfigure(), "app", "user", "create").getContent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownKindsAreRejected()
    {
        service.getTableDdl(new RuntimeConfigure(), "app", "user", "drop");
    }
}
