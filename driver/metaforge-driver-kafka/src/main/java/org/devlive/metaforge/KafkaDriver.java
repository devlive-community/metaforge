package org.devlive.metaforge;

import org.devlive.metaforge.driver.sql.ConnectionUrl;
import org.devlive.metaforge.driver.sql.SqlSource;
import org.devlive.metaforge.driver.sql.SqlSourceDriver;

import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * JDBC over Kafka: {@code jdbc:kafka://broker:port[,broker:port]}. The cluster is the single
 * database {@code kafka}, each topic a table whose rows are its records with the columns
 * partition, offset, timestamp, key and value. Options other than the driver's own
 * ({@code timeout}, {@code maxRows}) are passed to the Kafka clients, such as
 * {@code security.protocol}.
 */
public class KafkaDriver
        extends SqlSourceDriver
{
    static {
        try {
            DriverManager.registerDriver(new KafkaDriver());
        }
        catch (SQLException error) {
            throw new IllegalStateException("Can not register the Kafka JDBC driver", error);
        }
    }

    @Override
    protected String prefix()
    {
        return "jdbc:kafka://";
    }

    @Override
    protected SqlSource open(ConnectionUrl url)
            throws SQLException
    {
        return KafkaSource.connect(url);
    }
}
