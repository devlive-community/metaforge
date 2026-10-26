package org.devlive.metaforge.test.basic;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.sql.SQLParser;
import org.devlive.metaforge.sql.node.option.TableOption;
import org.devlive.metaforge.sql.statement.CreateTableStatement;
import org.devlive.metaforge.sql.statement.SQLStatement;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;

@SuppressFBWarnings(value = {"JUA_DONT_ASSERT_INSTANCEOF_IN_TESTS"})
public class CreateStatementTest
{
    @Test
    public void testFlinkSql()
    {
        String sql = "CREATE TABLE source_table (\n" +
                "  COL1 INT,\n" +
                "  COL2 TIMESTAMP(0),\n" +
                "  COL3 VARCHAR(14),\n" +
                "  COL4 CHAR(1),\n" +
                "  PRIMARY KEY (COL1) NOT ENFORCED\n" +
                ") WITH\n" +
                "  (\n" +
                "    'connector' = 'mysql',\n" +
                "    'hostname' = '127.0.0.1',\n" +
                "    'port' = '3306',\n" +
                "    'username' = 'root',\n" +
                "    'password' = 'root',\n" +
                "    'database' = 'default',\n" +
                "    'table' = 'example'\n" +
                "  );\n" +
                "\n" +
                "\n" +
                "CREATE TABLE transform_table (\n" +
                "  COL1 INT,\n" +
                "  COL2 TIMESTAMP(0),\n" +
                "  COL3 VARCHAR(14),\n" +
                "  COL4 CHAR(1),\n" +
                "  PRIMARY KEY (COL1) NOT ENFORCED\n" +
                ") WITH (\n" +
                "  'connector' = 'kafka',\n" +
                "  'topic' = 'sink-topic',\n" +
                "  'properties.bootstrap.servers' = '127.0.0.1:9092',\n" +
                "  'properties.group.id' = 'sink-consumer-group',\n" +
                "  'format' = 'debezium-json',\n" +
                "  'properties.request.timeout.ms' = '120000',\n" +
                "  'properties.session.timeout.ms' = '90000'\n" +
                ");\n" +
                "\n" +
                "INSERT INTO sink_table\n" +
                "SELECT\n" +
                "  COL1,\n" +
                "  COL2,\n" +
                "  COL3,\n" +
                "  COL4\n" +
                "FROM\n" +
                "  transform_table;";

        // 解析多个语句
        // Parse multiple statements
        List<SQLStatement> statements = SQLParser.parseMultiple(sql);

        System.out.println("Total statements parsed: " + statements.size());

        for (int i = 0; i < statements.size(); i++) {
            SQLStatement stmt = statements.get(i);
            System.out.println("Statement " + (i + 1) + ": " + stmt.getClass().getSimpleName());

            if (stmt instanceof CreateTableStatement) {
                CreateTableStatement createStmt = (CreateTableStatement) stmt;
                System.out.println("Table name: " + createStmt.getTableName());
                List<TableOption> options = createStmt.getOptions();
                System.out.println("Options: " + options);
                System.out.println("---");
            }
        }
    }

    @Test
    public void testSingleCreateTable()
    {
        String singleSql = "CREATE TABLE source_table (\n" +
                "  COL1 INT,\n" +
                "  COL2 TIMESTAMP(0),\n" +
                "  COL3 VARCHAR(14),\n" +
                "  COL4 CHAR(1),\n" +
                "  PRIMARY KEY (COL1) NOT ENFORCED\n" +
                ") WITH\n" +
                "  (\n" +
                "    'connector' = 'mysql',\n" +
                "    'hostname' = '127.0.0.1',\n" +
                "    'port' = '3306',\n" +
                "    'username' = 'root',\n" +
                "    'password' = 'root',\n" +
                "    'database' = 'default',\n" +
                "    'table' = 'example'\n" +
                "  );";

        SQLStatement stmt = SQLParser.parse(singleSql);
        assertTrue(stmt instanceof CreateTableStatement);
        CreateTableStatement statement = (CreateTableStatement) stmt;

        List<TableOption> options = statement.getOptions();
        System.out.println("Single statement options: " + options);
    }
}