package org.devlive.metaforge.sql.statement;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class DropTableStatement
        extends SQLStatement
{
    private final List<String> tableNames;
    private final boolean ifExists;

    public DropTableStatement(List<String> tableNames, boolean ifExists)
    {
        super(StatementType.DROP);
        this.tableNames = tableNames;
        this.ifExists = ifExists;
    }
}
