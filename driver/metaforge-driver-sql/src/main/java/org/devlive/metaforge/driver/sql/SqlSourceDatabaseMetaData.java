package org.devlive.metaforge.driver.sql;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.driver.sql.base.BaseDatabaseMetaData;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/** Databases are schemas: getSchemas, getTables, getColumns and getPrimaryKeys describe the source. */
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "JDBC objects share their connection, statement and source")
public class SqlSourceDatabaseMetaData
        extends BaseDatabaseMetaData
{
    private static final List<String> SCHEMA_LABELS = List.of("TABLE_SCHEM", "TABLE_CATALOG");
    private static final List<String> TABLE_LABELS = List.of("TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "TABLE_TYPE", "REMARKS",
            "TYPE_CAT", "TYPE_SCHEM", "TYPE_NAME", "SELF_REFERENCING_COL_NAME", "REF_GENERATION");
    private static final List<String> COLUMN_LABELS = List.of("TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "COLUMN_NAME", "DATA_TYPE", "TYPE_NAME",
            "COLUMN_SIZE", "BUFFER_LENGTH", "DECIMAL_DIGITS", "NUM_PREC_RADIX", "NULLABLE", "REMARKS", "COLUMN_DEF", "SQL_DATA_TYPE",
            "SQL_DATETIME_SUB", "CHAR_OCTET_LENGTH", "ORDINAL_POSITION", "IS_NULLABLE", "SCOPE_CATALOG", "SCOPE_SCHEMA", "SCOPE_TABLE",
            "SOURCE_DATA_TYPE", "IS_AUTOINCREMENT", "IS_GENERATEDCOLUMN");
    private static final List<String> COLUMN_TYPES = List.of("VARCHAR", "VARCHAR", "VARCHAR", "VARCHAR", "INTEGER", "VARCHAR",
            "INTEGER", "INTEGER", "INTEGER", "INTEGER", "INTEGER", "VARCHAR", "VARCHAR", "INTEGER",
            "INTEGER", "INTEGER", "INTEGER", "VARCHAR", "VARCHAR", "VARCHAR", "VARCHAR",
            "INTEGER", "VARCHAR", "VARCHAR");
    private static final List<String> KEY_LABELS = List.of("TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "COLUMN_NAME", "KEY_SEQ", "PK_NAME");

    private final SqlSourceConnection connection;

    public SqlSourceDatabaseMetaData(SqlSourceConnection connection)
    {
        this.connection = connection;
    }

    private SqlSource source()
    {
        return connection.getSource();
    }

    private static List<String> strings(int count)
    {
        String[] types = new String[count];
        Arrays.fill(types, "VARCHAR");
        return Arrays.asList(types);
    }

    /** A JDBC LIKE pattern; null or empty matches everything. */
    private static Pattern pattern(String like)
    {
        if (like == null || like.isEmpty()) {
            return Pattern.compile(".*", Pattern.DOTALL);
        }
        StringBuilder regex = new StringBuilder();
        for (char c : like.toCharArray()) {
            regex.append(c == '%' ? ".*" : c == '_' ? "." : Pattern.quote(String.valueOf(c)));
        }
        return Pattern.compile(regex.toString(), Pattern.DOTALL);
    }

    private List<String> databases(String schemaPattern)
            throws SQLException
    {
        Pattern pattern = pattern(schemaPattern);
        List<String> matched = new ArrayList<>();
        for (String database : source().databases()) {
            if (pattern.matcher(database).matches()) {
                matched.add(database);
            }
        }
        return matched;
    }

    @Override
    public ResultSet getSchemas()
            throws SQLException
    {
        return getSchemas(null, null);
    }

    @Override
    public ResultSet getSchemas(String catalog, String schemaPattern)
            throws SQLException
    {
        List<List<Object>> rows = new ArrayList<>();
        for (String database : databases(schemaPattern)) {
            rows.add(Arrays.asList(database, null));
        }
        return RowsResultSet.of(SCHEMA_LABELS, strings(2), rows);
    }

    @Override
    public ResultSet getCatalogs()
    {
        return RowsResultSet.of(List.of("TABLE_CAT"), strings(1), List.of());
    }

    @Override
    public ResultSet getTableTypes()
    {
        return RowsResultSet.of(List.of("TABLE_TYPE"), strings(1), List.of(List.of("TABLE")));
    }

    @Override
    public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types)
            throws SQLException
    {
        List<List<Object>> rows = new ArrayList<>();
        if (types == null || Arrays.asList(types).contains("TABLE")) {
            Pattern tables = pattern(tableNamePattern);
            for (String database : databases(schemaPattern)) {
                for (String table : source().tables(database)) {
                    if (tables.matcher(table).matches()) {
                        rows.add(Arrays.asList(null, database, table, "TABLE", "", null, null, null, null, null));
                    }
                }
            }
        }
        return RowsResultSet.of(TABLE_LABELS, strings(TABLE_LABELS.size()), rows);
    }

    @Override
    public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern)
            throws SQLException
    {
        List<List<Object>> rows = new ArrayList<>();
        Pattern tables = pattern(tableNamePattern);
        Pattern columns = pattern(columnNamePattern);
        for (String database : databases(schemaPattern)) {
            for (String table : source().tables(database)) {
                if (!tables.matcher(table).matches()) {
                    continue;
                }
                int position = 0;
                for (SourceColumn column : source().columns(database, table)) {
                    position++;
                    if (columns.matcher(column.getName()).matches()) {
                        rows.add(Arrays.asList(null, database, table, column.getName(), RowsResultSetMetaData.jdbcType(column.getType()), column.getType(),
                                0, null, 0, 10, column.isNullable() ? columnNullable : columnNoNulls, column.getComment(), null, null,
                                null, null, position, column.isNullable() ? "YES" : "NO", null, null, null,
                                null, "NO", "NO"));
                    }
                }
            }
        }
        return RowsResultSet.of(COLUMN_LABELS, COLUMN_TYPES, rows);
    }

    @Override
    public ResultSet getPrimaryKeys(String catalog, String schema, String table)
            throws SQLException
    {
        List<List<Object>> rows = new ArrayList<>();
        if (schema != null && table != null && source().databases().contains(schema) && source().tables(schema).contains(table)) {
            int sequence = 0;
            for (SourceColumn column : source().columns(schema, table)) {
                if (column.isKey()) {
                    rows.add(Arrays.asList(null, schema, table, column.getName(), ++sequence, "PRIMARY"));
                }
            }
        }
        return RowsResultSet.of(KEY_LABELS, List.of("VARCHAR", "VARCHAR", "VARCHAR", "VARCHAR", "INTEGER", "VARCHAR"), rows);
    }

    @Override
    public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate)
    {
        List<String> labels = List.of("TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "NON_UNIQUE", "INDEX_QUALIFIER", "INDEX_NAME", "TYPE",
                "ORDINAL_POSITION", "COLUMN_NAME", "ASC_OR_DESC", "CARDINALITY", "PAGES", "FILTER_CONDITION");
        return RowsResultSet.of(labels, strings(labels.size()), List.of());
    }

    @Override
    public ResultSet getImportedKeys(String catalog, String schema, String table)
    {
        List<String> labels = List.of("PKTABLE_CAT", "PKTABLE_SCHEM", "PKTABLE_NAME", "PKCOLUMN_NAME", "FKTABLE_CAT", "FKTABLE_SCHEM",
                "FKTABLE_NAME", "FKCOLUMN_NAME", "KEY_SEQ", "UPDATE_RULE", "DELETE_RULE", "FK_NAME", "PK_NAME", "DEFERRABILITY");
        return RowsResultSet.of(labels, strings(labels.size()), List.of());
    }

    @Override
    public ResultSet getExportedKeys(String catalog, String schema, String table)
    {
        return getImportedKeys(catalog, schema, table);
    }

    @Override
    public Connection getConnection()
    {
        return connection;
    }

    @Override
    public String getURL()
    {
        return connection.getUrl();
    }

    @Override
    public String getUserName()
    {
        return connection.getUser();
    }

    @Override
    public String getDatabaseProductName()
    {
        return source().productName();
    }

    @Override
    public String getDatabaseProductVersion()
            throws SQLException
    {
        return source().productVersion();
    }

    @Override
    public int getDatabaseMajorVersion()
            throws SQLException
    {
        return part(0);
    }

    @Override
    public int getDatabaseMinorVersion()
            throws SQLException
    {
        return part(1);
    }

    private int part(int index)
            throws SQLException
    {
        String[] parts = source().productVersion().split("[^0-9]+");
        List<String> numbers = new ArrayList<>();
        for (String part : parts) {
            if (!part.isEmpty()) {
                numbers.add(part);
            }
        }
        return index < numbers.size() ? Integer.parseInt(numbers.get(index)) : 0;
    }

    @Override
    public String getDriverName()
    {
        return "MetaForge " + source().productName() + " Driver";
    }

    @Override
    public String getDriverVersion()
    {
        return "1.0";
    }

    @Override
    public int getDriverMajorVersion()
    {
        return 1;
    }

    @Override
    public int getDriverMinorVersion()
    {
        return 0;
    }

    @Override
    public int getJDBCMajorVersion()
    {
        return 4;
    }

    @Override
    public int getJDBCMinorVersion()
    {
        return 2;
    }

    @Override
    public String getIdentifierQuoteString()
    {
        return "`";
    }

    @Override
    public String getSchemaTerm()
    {
        return "database";
    }

    @Override
    public String getCatalogTerm()
    {
        return "catalog";
    }

    @Override
    public String getCatalogSeparator()
    {
        return ".";
    }

    @Override
    public String getSearchStringEscape()
    {
        return "\\";
    }

    @Override
    public String getExtraNameCharacters()
    {
        return "";
    }

    @Override
    public String getSQLKeywords()
    {
        return "";
    }

    @Override
    public boolean isReadOnly()
    {
        return connection.isReadOnly();
    }

    @Override
    public boolean supportsTransactions()
    {
        return false;
    }

    @Override
    public int getDefaultTransactionIsolation()
    {
        return Connection.TRANSACTION_NONE;
    }

    @Override
    public boolean supportsTransactionIsolationLevel(int level)
    {
        return level == Connection.TRANSACTION_NONE;
    }

    @Override
    public boolean supportsMultipleResultSets()
    {
        return false;
    }

    @Override
    public boolean supportsBatchUpdates()
    {
        return false;
    }

    @Override
    public boolean supportsSchemasInDataManipulation()
    {
        return true;
    }

    @Override
    public boolean supportsSchemasInTableDefinitions()
    {
        return true;
    }

    @Override
    public boolean supportsCatalogsInDataManipulation()
    {
        return false;
    }

    @Override
    public boolean supportsCatalogsInTableDefinitions()
    {
        return false;
    }

    @Override
    public boolean supportsResultSetType(int type)
    {
        return type == ResultSet.TYPE_FORWARD_ONLY;
    }

    @Override
    public boolean supportsResultSetConcurrency(int type, int concurrency)
    {
        return type == ResultSet.TYPE_FORWARD_ONLY && concurrency == ResultSet.CONCUR_READ_ONLY;
    }

    @Override
    public boolean supportsStoredProcedures()
    {
        return false;
    }

    @Override
    public boolean supportsGetGeneratedKeys()
    {
        return false;
    }

    @Override
    public boolean usesLocalFiles()
    {
        return false;
    }

    @Override
    public boolean nullsAreSortedLow()
    {
        return true;
    }

    @Override
    public boolean storesMixedCaseIdentifiers()
    {
        return true;
    }

    @Override
    public boolean supportsMixedCaseIdentifiers()
    {
        return true;
    }

    @Override
    public RowIdLifetime getRowIdLifetime()
    {
        return RowIdLifetime.ROWID_UNSUPPORTED;
    }

    @Override
    public int getMaxConnections()
    {
        return 0;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface)
    {
        return iface.isInstance(this);
    }

    @Override
    public <T> T unwrap(Class<T> iface)
            throws SQLException
    {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        throw new SQLException("Not a wrapper for " + iface.getName());
    }
}
