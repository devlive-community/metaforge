package org.devlive.metaforge.sql.node.clause;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;
import org.devlive.metaforge.sql.node.option.ReferenceOption;

@Getter
@Setter
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class ForeignKeyClause
{
    private final String referencedTable;
    private final String[] referencedColumns;
    private final ReferenceOption onDelete;
    private final ReferenceOption onUpdate;

    public ForeignKeyClause(String referencedTable, String[] referencedColumns,
            ReferenceOption onDelete, ReferenceOption onUpdate)
    {
        this.referencedTable = referencedTable;
        this.referencedColumns = referencedColumns;
        this.onDelete = onDelete;
        this.onUpdate = onUpdate;
    }
}
