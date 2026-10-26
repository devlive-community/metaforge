package org.devlive.metaforge.spi.adapter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.connection.JdbcConnection;
import org.devlive.metaforge.spi.model.Configure;
import org.devlive.metaforge.spi.model.Response;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@SuppressFBWarnings(value = {"OBL_UNSATISFIED_OBLIGATION", "OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE"})
public final class JdbcStreamAdapter
{
    private static final int DEFAULT_FETCH_SIZE = 1000;
    private static final int DEFAULT_BATCH_SIZE = 1000;

    private JdbcStreamAdapter() {}

    public static void executeStream(
            PluginService plugin,
            Configure configure,
            String sql,
            int fetchSize,
            RowCallback callback)
    {
        int effectiveFetchSize = fetchSize > 0 ? fetchSize : DEFAULT_FETCH_SIZE;
        JdbcConnection jdbcConnection = openConnection(plugin, configure);
        try {
            Connection connection = (Connection) jdbcConnection.getConnection();
            if (connection == null) {
                throw new IllegalStateException("Open jdbc connection failed: "
                        + Optional.ofNullable(jdbcConnection.getResponse().getMessage()).orElse("unknown"));
            }

            boolean restoreAutoCommit = false;
            boolean originalAutoCommit = true;
            try {
                originalAutoCommit = connection.getAutoCommit();
                if (originalAutoCommit) {
                    connection.setAutoCommit(false);
                    restoreAutoCommit = true;
                }
            }
            catch (SQLException ignore) {
                // Some drivers (e.g. read-only HTTP based) do not support autoCommit toggling; just continue.
            }

            try (Statement statement = connection.createStatement(
                    ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY)) {
                applyFetchSize(statement, effectiveFetchSize, configure);
                // 暴露给上层，方便取消逻辑直接调用 Statement.cancel()
                callback.onStatement(statement);
                try (ResultSet rs = statement.executeQuery(sql)) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    List<String> headers = new ArrayList<>(columnCount);
                    List<String> types = new ArrayList<>(columnCount);
                    for (int i = 1; i <= columnCount; i++) {
                        String label = metaData.getColumnLabel(i);
                        headers.add(label != null && !label.isEmpty() ? label : metaData.getColumnName(i));
                        types.add(metaData.getColumnTypeName(i));
                    }
                    callback.onSchema(headers, types);
                    while (rs.next()) {
                        List<Object> row = new ArrayList<>(columnCount);
                        for (int i = 1; i <= columnCount; i++) {
                            row.add(rs.getObject(i));
                        }
                        callback.onRow(row);
                    }
                }
            }
            finally {
                if (restoreAutoCommit) {
                    try {
                        connection.setAutoCommit(originalAutoCommit);
                    }
                    catch (SQLException ignore) {
                    }
                }
            }
        }
        catch (SQLException ex) {
            throw new IllegalStateException("Stream read failed: " + ex.getMessage(), ex);
        }
        finally {
            jdbcConnection.destroy();
        }
    }

    public static BatchWriter openBatchWriter(
            PluginService plugin,
            Configure configure,
            String database,
            String table,
            List<String> columns,
            int batchSize)
    {
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("Batch writer requires at least one column");
        }
        int effectiveBatchSize = batchSize > 0 ? batchSize : DEFAULT_BATCH_SIZE;
        JdbcConnection jdbcConnection = openConnection(plugin, configure);
        Connection connection = (Connection) jdbcConnection.getConnection();
        if (connection == null) {
            jdbcConnection.destroy();
            throw new IllegalStateException("Open jdbc connection failed: "
                    + Optional.ofNullable(jdbcConnection.getResponse().getMessage()).orElse("unknown"));
        }
        return new JdbcBatchWriter(jdbcConnection, connection, database, table, columns, effectiveBatchSize);
    }

    private static JdbcConnection openConnection(PluginService plugin, Configure configure)
    {
        Response response = new Response();
        configure.setDriver(plugin.driver());
        configure.setType(plugin.connectType());
        configure.setUrl(Optional.of(plugin.url(configure)));
        return new JdbcConnection(configure, response);
    }

    private static void applyFetchSize(Statement statement, int fetchSize, Configure configure)
    {
        try {
            String type = configure.getType();
            // MySQL only streams when fetchSize is Integer.MIN_VALUE with TYPE_FORWARD_ONLY + CONCUR_READ_ONLY.
            // The plugin's connectType is "metaforge"; the original driver type is in the plugin itself, but the URL
            // encodes the actual database. Detect by URL prefix.
            String url = configure.getUrl().orElse("");
            if (url.startsWith("jdbc:mysql") || url.startsWith("jdbc:mariadb")) {
                statement.setFetchSize(Integer.MIN_VALUE);
            }
            else {
                statement.setFetchSize(fetchSize);
            }
            if (type != null && type.toLowerCase().contains("postgres")) {
                // PostgreSQL also needs autoCommit=false for cursor-based fetch (handled by caller).
                statement.setFetchSize(fetchSize);
            }
        }
        catch (SQLException ex) {
            log.warn("Set fetch size failed, falling back to driver default: {}", ex.getMessage());
        }
    }

    private static final class JdbcBatchWriter
            implements BatchWriter
    {
        private final JdbcConnection jdbcConnection;
        private final Connection connection;
        private final PreparedStatement statement;
        private final int columnCount;
        private final int batchSize;
        private int pending;
        private long written;
        private boolean originalAutoCommit = true;
        private boolean restoreAutoCommit;

        JdbcBatchWriter(
                JdbcConnection jdbcConnection,
                Connection connection,
                String database,
                String table,
                List<String> columns,
                int batchSize)
        {
            this.jdbcConnection = jdbcConnection;
            this.connection = connection;
            this.columnCount = columns.size();
            this.batchSize = batchSize;

            try {
                this.originalAutoCommit = connection.getAutoCommit();
                if (this.originalAutoCommit) {
                    connection.setAutoCommit(false);
                    this.restoreAutoCommit = true;
                }
            }
            catch (SQLException ignore) {
            }

            String sql = buildInsertTemplate(database, table, columns);
            try {
                this.statement = connection.prepareStatement(sql);
            }
            catch (SQLException ex) {
                safeClose();
                throw new IllegalStateException("Prepare insert failed: " + ex.getMessage(), ex);
            }
        }

        @Override
        public void addRow(List<?> row)
        {
            if (row == null || row.size() != columnCount) {
                throw new IllegalArgumentException(
                        "Row size " + (row == null ? -1 : row.size()) + " does not match column count " + columnCount);
            }
            try {
                for (int i = 0; i < columnCount; i++) {
                    Object value = row.get(i);
                    if (value == null) {
                        statement.setObject(i + 1, null);
                    }
                    else {
                        statement.setObject(i + 1, value);
                    }
                }
                statement.addBatch();
                pending++;
                if (pending >= batchSize) {
                    flush();
                }
            }
            catch (SQLException ex) {
                throw new IllegalStateException("Add batch row failed: " + ex.getMessage(), ex);
            }
        }

        @Override
        public long writtenCount()
        {
            return written;
        }

        @Override
        public void close()
        {
            try {
                if (pending > 0) {
                    flush();
                }
            }
            catch (SQLException ex) {
                throw new IllegalStateException("Flush final batch failed: " + ex.getMessage(), ex);
            }
            finally {
                safeClose();
            }
        }

        private void flush()
                throws SQLException
        {
            statement.executeBatch();
            connection.commit();
            written += pending;
            pending = 0;
            statement.clearBatch();
        }

        private void safeClose()
        {
            try {
                if (statement != null) {
                    statement.close();
                }
            }
            catch (SQLException ex) {
                log.warn("Close prepared statement failed: {}", ex.getMessage());
            }
            try {
                if (restoreAutoCommit) {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
            catch (SQLException ignore) {
            }
            jdbcConnection.destroy();
        }
    }

    private static String buildInsertTemplate(String database, String table, List<String> columns)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ");
        if (database != null && !database.isEmpty()) {
            sb.append('`').append(database).append("`.");
        }
        sb.append('`').append(table).append("` (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append('`').append(columns.get(i)).append('`');
        }
        sb.append(") VALUES (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append('?');
        }
        sb.append(')');
        return sb.toString();
    }
}
