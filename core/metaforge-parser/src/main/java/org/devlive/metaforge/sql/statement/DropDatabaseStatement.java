package org.devlive.metaforge.sql.statement;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class DropDatabaseStatement
        extends SQLStatement
{
    private final String databaseName;
    private final boolean ifNotExists;

    public DropDatabaseStatement(String databaseName, boolean ifNotExists)
    {
        super(StatementType.DROP);
        this.databaseName = databaseName;
        this.ifNotExists = ifNotExists;
    }
}
