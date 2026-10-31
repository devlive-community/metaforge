package org.devlive.metaforge.spi;

import org.devlive.metaforge.spi.sql.SqlStatements;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class SqlStatementsTest
{
    private static final String TRIGGER = "CREATE TRIGGER `datacap`.`trg_bi`\nBEFORE INSERT ON `datacap`.`relation`\nFOR EACH ROW\nBEGIN\n"
            + "    DECLARE s1 VARCHAR(40) CHARACTER SET utf8;\n    DECLARE s2 VARCHAR(20) CHARACTER SET utf8;\n    SET s2 = ' is created';\n"
            + "    SET s1 = CONCAT(NEW.name, s2);\n    INSERT INTO `datacap`.`logs`(`log`) VALUES (s1);\nEND";

    @Test
    public void compoundBodiesStayOneStatement()
    {
        Assert.assertEquals(Collections.singletonList(TRIGGER), SqlStatements.split(TRIGGER + ";"));
        Assert.assertEquals(Arrays.asList("DROP TRIGGER `datacap`.`trg_bi`", TRIGGER, "SELECT 1"), SqlStatements.split("DROP TRIGGER `datacap`.`trg_bi`;\n" + TRIGGER + ";\nSELECT 1;"));
    }

    @Test
    public void blocksInsideBodiesAreTracked()
    {
        String body = "CREATE TRIGGER t BEFORE INSERT ON x FOR EACH ROW BEGIN\n  IF NEW.a IS NULL THEN SET NEW.a = 1; END IF;\n"
                + "  SET NEW.b = CASE WHEN NEW.c > 0 THEN 'p;' ELSE 'n' END;\n  WHILE NEW.d < 3 DO SET NEW.d = NEW.d + 1; END WHILE;\nEND";
        Assert.assertEquals(Arrays.asList(body, "UPDATE x SET a = 2"), SqlStatements.split(body + ";\nUPDATE x SET a = 2;"));
    }

    @Test
    public void quotedTextAndCommentsDoNotSplit()
    {
        Assert.assertEquals(Arrays.asList("INSERT INTO t VALUES ('a;b', \"c;\", 'it''s;')", "-- note; here\nUPDATE t SET `x;y` = 1 /* ; */"),
                SqlStatements.split("INSERT INTO t VALUES ('a;b', \"c;\", 'it''s;');\n-- note; here\nUPDATE t SET `x;y` = 1 /* ; */;"));
        Assert.assertEquals(Arrays.asList("BEGIN", "INSERT INTO t VALUES (1)", "COMMIT"), SqlStatements.split("BEGIN; INSERT INTO t VALUES (1); COMMIT;"));
        Assert.assertEquals(Arrays.asList("SELECT CASE WHEN a THEN 1 END FROM t", "SELECT 2"), SqlStatements.split("SELECT CASE WHEN a THEN 1 END FROM t; SELECT 2"));
    }
}
