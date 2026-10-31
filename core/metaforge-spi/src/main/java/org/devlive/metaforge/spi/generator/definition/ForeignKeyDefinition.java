package org.devlive.metaforge.spi.generator.definition;

import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@ToString
@NoArgsConstructor
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "Definition holders intentionally expose mutable lists to the DDL builders")
public class ForeignKeyDefinition
{
    private String name;
    // Columns of this table, aligned with referencedColumns.
    private List<String> columns = Lists.newArrayList();
    // Database of the referenced table; empty means the table's own database.
    private String referencedDatabase;
    private String referencedTable;
    private List<String> referencedColumns = Lists.newArrayList();
    // RESTRICT, CASCADE, SET NULL or NO ACTION; empty keeps the database default.
    private String onUpdate;
    private String onDelete;
    // The foreign key being edited or rebuilt; it is dropped before the new definition is added.
    private String originalName;
}
