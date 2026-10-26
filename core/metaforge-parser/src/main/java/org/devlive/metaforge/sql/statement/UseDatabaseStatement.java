package org.devlive.metaforge.sql.statement;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class UseDatabaseStatement
        extends SQLStatement
{
    private final String databaseName;

    public UseDatabaseStatement(String databaseName)
    {
        super(StatementType.USE);
        this.databaseName = databaseName;
    }
}
