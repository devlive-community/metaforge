package org.devlive.metaforge.plugin;

import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.spi.adapter.JdbcAdapter;
import org.devlive.metaforge.spi.column.JdbcColumn;
import org.devlive.metaforge.spi.connection.JdbcConnection;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.devlive.metaforge.spi.model.Time;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Slf4j
@SuppressFBWarnings(value = {"RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE"},
        justification = "I prefer to suppress these FindBugs warnings")
public class KyuubiAdapter
        extends JdbcAdapter
{
    public KyuubiAdapter(JdbcConnection jdbcConnection)
    {
        super(jdbcConnection);
    }

    @Override
    public Response handlerExecute(String content)
    {
        Time processorTime = new Time();
        processorTime.setStart(new Date().getTime());
        Response response = this.connection.getResponse();
        Connection connection = (Connection) this.connection.getConnection();
        RuntimeConfigure configure = this.connection.getConfigure();
        if (response.getIsConnected()) {
            try (PreparedStatement statement = connection.prepareStatement(content)) {
                List<String> headers = new ArrayList<>();
                List<String> types = new ArrayList<>();
                List<Object> columns = new ArrayList<>();
                // Split SQL
                List<String> setBuffer = Lists.newArrayList();
                List<String> buffer = Lists.newArrayList();
                Arrays.stream(content.split("\n"))
                        .forEach(line -> {
                            if (line.trim().toUpperCase().startsWith("SET ")) {
                                setBuffer.add(line);
                            }
                            else {
                                buffer.add(line);
                            }
                        });
                // Execute SET
                for (String sql : setBuffer) {
                    statement.execute(sql);
                }
                try (PreparedStatement resetStatement = connection.prepareStatement(String.join("\n", buffer));
                        ResultSet resultSet = resetStatement.executeQuery()) {
                    // Reset query SQL
                    boolean isPresent = true;
                    JdbcColumn jdbcColumn = new JdbcColumn(resultSet);
                    while (resultSet.next()) {
                        ResultSetMetaData metaData = resultSet.getMetaData();
                        int columnCount = metaData.getColumnCount();
                        List<Object> _columns = new ArrayList<>();
                        for (int i = 1; i <= columnCount; i++) {
                            if (isPresent) {
                                headers.add(metaData.getColumnName(i));
                                types.add(metaData.getColumnTypeName(i));
                            }
                            _columns.add(jdbcColumn.mappingColumnData(metaData.getColumnTypeName(i), i));
                        }
                        isPresent = false;
                        columns.add(_columns);
                    }
                }
                catch (SQLException tryUpdateEx) {
                    if (Objects.equals(tryUpdateEx.getSQLState(), "S1009")) {
                        try {
                            headers.add("result");
                            types.add(Integer.class.getSimpleName());
                            List<Object> _columns = new ArrayList<>();
                            _columns.add(statement.executeUpdate());
                            columns.add(_columns);
                        }
                        catch (SQLException updateEx) {
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
        this.connection.destroy();
        return response;
    }
}
