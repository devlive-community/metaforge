package org.devlive.metaforge.sql.statement;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import org.devlive.metaforge.sql.node.element.ColumnElement;

import java.util.List;

/** ALTER TABLE with its column changes, in the order written. */
@Getter
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class AlterTableStatement
        extends SQLStatement
{
    private final String tableName;
    private final List<Action> actions;

    public AlterTableStatement(String tableName, List<Action> actions)
    {
        super(StatementType.ALTER);
        this.tableName = tableName;
        this.actions = actions;
    }

    public enum ActionType
    {
        ADD_COLUMN, DROP_COLUMN, MODIFY_COLUMN, RENAME_COLUMN, RENAME_TABLE, OTHER
    }

    /** One change: the column added or modified, the column dropped or renamed, or the new name. */
    @Getter
    @SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
    public static class Action
    {
        private final ActionType type;
        private final ColumnElement column;
        private final String columnName;
        private final String newName;

        public Action(ActionType type, ColumnElement column, String columnName, String newName)
        {
            this.type = type;
            this.column = column;
            this.columnName = columnName;
            this.newName = newName;
        }
    }
}
