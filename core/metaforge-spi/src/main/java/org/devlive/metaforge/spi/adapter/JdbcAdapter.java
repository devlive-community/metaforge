package org.devlive.metaforge.spi.adapter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.spi.column.Column;
import org.devlive.metaforge.spi.column.JdbcColumn;
import org.devlive.metaforge.spi.connection.JdbcConnection;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.devlive.metaforge.spi.model.Time;
import org.devlive.metaforge.spi.sql.SqlStatements;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@SuppressFBWarnings(value = {"RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE"})
public class JdbcAdapter
        implements Adapter
{
    protected org.devlive.metaforge.spi.connection.Connection connection;

    public JdbcAdapter(org.devlive.metaforge.spi.connection.Connection connection)
    {
        this.connection = connection;
    }

    @Override
    public Response handlerExecute(String content)
    {
        JdbcConnection jdbcConnection = (JdbcConnection) this.connection;
        Time processorTime = new Time();
        processorTime.setStart(new Date().getTime());
        Response response = jdbcConnection.getResponse();
        Connection connection = (Connection) jdbcConnection.getConnection();
        RuntimeConfigure configure = jdbcConnection.getConfigure();
        log.debug("Execute sql \n{}", content);
        if (response.getIsConnected()) {
            try (Statement statement = connection.createStatement()) {
                List<String> headers = new ArrayList<>();
                List<String> types = new ArrayList<>();
                List<Object> columns = new ArrayList<>();
                try {
                    // A single statement runs as written, so the driver decides whether it returns rows;
                    // a script runs statement by statement. Detecting non-queries through executeQuery's
                    // error only works with MySQL's driver.
                    List<String> parts = new ArrayList<>();
                    for (String part : SqlStatements.split(content)) {
                        if (!part.trim().isEmpty()) {
                            parts.add(part);
                        }
                    }
                    if (parts.size() <= 1 && statement.execute(content)) {
                        try (ResultSet resultSet = statement.getResultSet()) {
                            Column jdbcColumn = new JdbcColumn(resultSet);
                            ResultSetMetaData metaData = resultSet.getMetaData();
                            int columnCount = metaData.getColumnCount();
                            for (int i = 1; i <= columnCount; i++) {
                                headers.add(metaData.getColumnLabel(i));
                                types.add(jdbcColumn.mappingColumnType(metaData.getColumnTypeName(i)));
                            }
                            while (resultSet.next()) {
                                List<Object> _columns = new ArrayList<>();
                                for (int i = 1; i <= columnCount; i++) {
                                    _columns.add(jdbcColumn.mappingColumnData(metaData.getColumnTypeName(i), i));
                                }
                                columns.add(_columns);
                            }
                        }
                    }
                    else {
                        headers.add("result");
                        types.add(Integer.class.getSimpleName());
                        List<Object> _columns = new ArrayList<>();
                        _columns.add(parts.size() <= 1 ? Math.max(statement.getUpdateCount(), 0) : runScript(connection, statement, parts));
                        columns.add(_columns);
                    }
                }
                finally {
                    response.setHeaders(headers);
                    response.setTypes(types);
                    response.setColumns(handlerFormatter(configure.getPluginManager(), configure.getFormat(), headers, columns));
                    response.setIsSuccessful(Boolean.TRUE);
                }
            }
            catch (SQLException ex) {
                log.error("Execute content failed content {} exception ", content, ex);
                response.setIsSuccessful(Boolean.FALSE);
                response.setMessage(ex.getMessage());
            }
        }
        processorTime.setEnd(new Date().getTime());
        response.setProcessor(processorTime);
        // It will be destroyed after the mission is closed
        jdbcConnection.destroy();
        return response;
    }

    /**
     * Runs a script's statements in order, as one transaction where the driver supports
     * transactions (ClickHouse, for one, does not), and returns the rows they changed.
     */
    private static int runScript(Connection connection, Statement statement, List<String> parts)
            throws SQLException
    {
        boolean transactional = connection.getMetaData().supportsTransactions();
        boolean autoCommit = connection.getAutoCommit();
        if (transactional) {
            connection.setAutoCommit(false);
        }
        try {
            int count = 0;
            for (String part : parts) {
                if (!statement.execute(part)) {
                    count += Math.max(statement.getUpdateCount(), 0);
                }
            }
            if (transactional) {
                connection.commit();
            }
            return count;
        }
        catch (SQLException updateEx) {
            if (transactional) {
                try {
                    connection.rollback();
                }
                catch (SQLException rollbackEx) {
                    log.error("Rollback failed ", rollbackEx);
                    updateEx.addSuppressed(rollbackEx);
                }
            }
            throw updateEx;
        }
        finally {
            if (transactional) {
                connection.setAutoCommit(autoCommit);
            }
        }
    }
}
