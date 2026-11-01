package org.devlive.metaforge.service.importer;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayList;
import java.util.List;

/** One table found in an import file: its columns, the CREATE statement when the file has one, and its rows. */
@SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Readers fill the columns and rows in place")
public final class ImportTable
{
    private final String name;
    private final List<ImportColumn> columns = new ArrayList<>();
    private final List<List<Object>> rows = new ArrayList<>();
    private String definition;

    public ImportTable(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public List<ImportColumn> getColumns()
    {
        return columns;
    }

    public List<List<Object>> getRows()
    {
        return rows;
    }

    public String getDefinition()
    {
        return definition;
    }

    public void setDefinition(String definition)
    {
        this.definition = definition;
    }

    public int columnIndex(String column)
    {
        for (int index = 0; index < columns.size(); index++) {
            if (columns.get(index).getName().equals(column)) {
                return index;
            }
        }
        return -1;
    }

    /** A column of an import table: name, declared or inferred type and a few sample values. */
    public static final class ImportColumn
    {
        private final String name;
        private String type;

        public ImportColumn(String name, String type)
        {
            this.name = name;
            this.type = type;
        }

        public String getName()
        {
            return name;
        }

        public String getType()
        {
            return type;
        }

        public void setType(String type)
        {
            this.type = type;
        }
    }
}
