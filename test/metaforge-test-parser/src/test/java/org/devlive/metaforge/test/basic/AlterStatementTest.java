package org.devlive.metaforge.test.basic;

import org.devlive.metaforge.sql.SQLParser;
import org.devlive.metaforge.sql.statement.AlterTableStatement;
import org.devlive.metaforge.sql.statement.AlterTableStatement.ActionType;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

public class AlterStatementTest
{
    @Test
    public void columnChangesAreReadInOrder()
    {
        AlterTableStatement statement = (AlterTableStatement) SQLParser.parse(
                "ALTER TABLE app.config ADD COLUMN timeout VARCHAR(16), MODIFY COLUMN host TEXT, DROP COLUMN port, RENAME COLUMN name TO title");
        Assert.assertEquals("app.config", statement.getTableName());
        Assert.assertEquals(List.of(ActionType.ADD_COLUMN, ActionType.MODIFY_COLUMN, ActionType.DROP_COLUMN, ActionType.RENAME_COLUMN),
                statement.getActions().stream().map(AlterTableStatement.Action::getType).collect(Collectors.toList()));
        Assert.assertEquals("timeout", statement.getActions().get(0).getColumnName());
        Assert.assertEquals("VARCHAR", statement.getActions().get(0).getColumn().getDataType().getBaseType().toUpperCase());
        Assert.assertEquals("port", statement.getActions().get(2).getColumnName());
        Assert.assertEquals("title", statement.getActions().get(3).getNewName());
    }

    @Test
    public void aTableCanBeRenamed()
    {
        AlterTableStatement statement = (AlterTableStatement) SQLParser.parse("ALTER TABLE logs RENAME TO archive");
        Assert.assertEquals(ActionType.RENAME_TABLE, statement.getActions().get(0).getType());
        Assert.assertEquals("archive", statement.getActions().get(0).getNewName());
    }
}
