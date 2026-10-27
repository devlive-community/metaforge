package org.devlive.metaforge.spi.adapter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.spi.column.Column;
import org.devlive.metaforge.spi.column.JdbcColumn;
import org.devlive.metaforge.spi.connection.JdbcConnection;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.devlive.metaforge.spi.model.Time;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

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
                try (ResultSet resultSet = statement.executeQuery(content)) {
                    Column jdbcColumn = new JdbcColumn(resultSet);
                    ResultSetMetaData metaData = resultSet.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    for (int i = 1; i <= columnCount; i++) {
                        headers.add(metaData.getColumnName(i));
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
                catch (SQLException tryUpdateEx) {
                    if (Objects.equals(tryUpdateEx.getSQLState(), "S1009")) {
                        try {
                            headers.add("result");
                            types.add(Integer.class.getSimpleName());
                            List<Object> _columns = new ArrayList<>();
                            connection.setAutoCommit(false);
                            String[] parts = content.replaceAll("[\\r\\n|\\r|\\n]+", " ")
                                    .split("(?<=\\);)|(?<=\\r\\n)|(?<=\\r)|(?<=\\n)|(?<=\\n;)|(?<=\\r;)|(?<=\\r\\n;)|(?<=;)");
                            int count = 0;
                            for (String part : parts) {
                                if (!part.trim().isEmpty()) {
                                    count += statement.executeUpdate(part);
                                }
                            }
                            _columns.add(count);
                            connection.commit();
                            columns.add(_columns);
                        }
                        catch (SQLException updateEx) {
                            try {
                                connection.rollback();
                            }
                            catch (SQLException rollbackEx) {
                                log.error("Rollback failed ", rollbackEx);
                                throw new SQLException(rollbackEx);
                            }
                            throw new SQLException(updateEx);
                        }
                    }
                    else {
                        throw new SQLException(tryUpdateEx);
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
}
