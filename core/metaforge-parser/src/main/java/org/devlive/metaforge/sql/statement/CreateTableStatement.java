package org.devlive.metaforge.sql.statement;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.devlive.metaforge.sql.node.element.TableElement;
import org.devlive.metaforge.sql.node.option.TableOption;

import java.util.List;

@Getter
@Setter
@ToString
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class CreateTableStatement
        extends SQLStatement
{
    private final String tableName;
    private final boolean temporary;
    private final boolean ifNotExists;
    private final List<TableElement> columns;
    private final List<TableOption> options;

    public CreateTableStatement(
            String tableName,
            boolean temporary,
            boolean ifNotExists,
            List<TableElement> columns,
            List<TableOption> options
    )
    {
        super(StatementType.CREATE);
        this.tableName = tableName;
        this.temporary = temporary;
        this.ifNotExists = ifNotExists;
        this.columns = columns;
        this.options = options;
    }
}
