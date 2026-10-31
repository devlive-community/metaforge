package org.devlive.metaforge.driver.sql;

import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * A source without SQL seen as databases, tables and columns. The driver parses SQL and calls
 * these; what a source can not do it leaves to the defaults, which refuse with the statement
 * named, so the driver documents its SQL by what it implements.
 */
public interface SqlSource
        extends AutoCloseable
{
    String productName();

    String productVersion()
            throws SQLException;

    List<String> databases()
            throws SQLException;

    default void createDatabase(String database)
            throws SQLException
    {
        throw unsupported("CREATE DATABASE");
    }

    default void dropDatabase(String database)
            throws SQLException
    {
        throw unsupported("DROP DATABASE");
    }

    List<String> tables(String database)
            throws SQLException;

    default void createTable(String database, String table, List<SourceColumn> columns)
            throws SQLException
    {
        throw unsupported("CREATE TABLE");
    }

    /** CREATE TABLE with its options, such as WITH ('partitions' = '3'); sources without options ignore them. */
    default void createTable(String database, String table, List<SourceColumn> columns, Map<String, String> options)
            throws SQLException
    {
        createTable(database, table, columns);
    }

    default void dropTable(String database, String table)
            throws SQLException
    {
        throw unsupported("DROP TABLE");
    }

    List<SourceColumn> columns(String database, String table)
            throws SQLException;

    default void addColumn(String database, String table, SourceColumn column)
            throws SQLException
    {
        throw unsupported("ALTER TABLE ... ADD COLUMN");
    }

    default void modifyColumn(String database, String table, SourceColumn column)
            throws SQLException
    {
        throw unsupported("ALTER TABLE ... MODIFY COLUMN");
    }

    default void dropColumn(String database, String table, String column)
            throws SQLException
    {
        throw unsupported("ALTER TABLE ... DROP COLUMN");
    }

    default void renameColumn(String database, String table, String column, String name)
            throws SQLException
    {
        throw unsupported("ALTER TABLE ... RENAME COLUMN");
    }

    /**
     * The table's rows, values in the order of {@link #columns}. {@code limit} is a hint for
     * sources that read lazily: the driver needs at most that many rows when no filter or order
     * is given, and -1 means all.
     */
    List<List<Object>> rows(String database, String table, long limit)
            throws SQLException;

    default int insert(String database, String table, List<String> columns, List<List<Object>> rows)
            throws SQLException
    {
        throw unsupported("INSERT");
    }

    /** Sets values on the rows the filter accepts; a row is given as column name to value. */
    default int update(String database, String table, Map<String, Object> values, Predicate<Map<String, Object>> filter)
            throws SQLException
    {
        throw unsupported("UPDATE");
    }

    default int delete(String database, String table, Predicate<Map<String, Object>> filter)
            throws SQLException
    {
        throw unsupported("DELETE");
    }

    @Override
    void close()
            throws SQLException;

    static SQLException unsupported(String statement)
    {
        return new SQLFeatureNotSupportedException(statement + " is not supported by this source");
    }
}
