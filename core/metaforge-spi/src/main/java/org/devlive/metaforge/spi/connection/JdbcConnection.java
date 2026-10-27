package org.devlive.metaforge.spi.connection;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.devlive.metaforge.plugin.PluginContextManager;
import org.devlive.metaforge.plugin.loader.PluginClassLoader;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

@Slf4j
public class JdbcConnection
        extends org.devlive.metaforge.spi.connection.Connection
{
    private java.sql.Connection jdbcConnection;

    public JdbcConnection(RuntimeConfigure configure, Response response)
    {
        super(configure, response);
    }

    protected java.sql.Connection openConnection()
    {
        try {
            PluginClassLoader pluginClassLoader = configure.getPlugin().getPluginClassLoader();
            PluginContextManager.runWithClassLoader(pluginClassLoader, () -> {
                Class<?> driverClass = Class.forName(configure.getDriver(), true, pluginClassLoader);
                Driver driver = (Driver) driverClass.getDeclaredConstructor().newInstance();
                DriverManager.registerDriver(new DriverShim(driver));

                if (configure.getUrl().isEmpty()) {
                    throw new RuntimeException("Connection url not present");
                }

                String url = configure.getUrl().get();
                log.info("Connection driver {}", configure.getType());
                log.info("Connection url {}", url);
                if (configure.getUsername().isPresent() && configure.getPassword().isPresent()) {
                    log.info("Connection username with {} password with {}",
                            configure.getUsername().get(), "***");
                    this.jdbcConnection = DriverManager.getConnection(
                            url,
                            configure.getUsername().get(),
                            configure.getPassword().get()
                    );
                }
                else {
                    log.info("Connection username and password not present");
                    Properties properties = new Properties();
                    if (configure.getUsername().isPresent()) {
                        properties.put("user", configure.getUsername().get());
                    }
                    this.jdbcConnection = DriverManager.getConnection(url, properties);
                }
                response.setIsConnected(Boolean.TRUE);

                return null;
            });
        }
        catch (Exception ex) {
            log.error("Connection failed ", ex);
            response.setIsConnected(Boolean.FALSE);
            response.setMessage(ex.getMessage());
        }
        return this.jdbcConnection;
    }

    public void destroy()
    {
        if (ObjectUtils.isNotEmpty(this.jdbcConnection)) {
            try {
                this.jdbcConnection.close();
            }
            catch (SQLException ex) {
                log.error("Connection close failed ", ex);
            }
        }
    }

    private static class DriverShim
            implements Driver
    {
        private final Driver driver;

        DriverShim(Driver d)
        {
            this.driver = d;
        }

        public Connection connect(String url, Properties info)
                throws SQLException
        {
            return driver.connect(url, info);
        }

        public boolean acceptsURL(String url)
                throws SQLException
        {
            return driver.acceptsURL(url);
        }

        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info)
                throws SQLException
        {
            return driver.getPropertyInfo(url, info);
        }

        public int getMajorVersion()
        {
            return driver.getMajorVersion();
        }

        public int getMinorVersion()
        {
            return driver.getMinorVersion();
        }

        public boolean jdbcCompliant()
        {
            return driver.jdbcCompliant();
        }

        @Override
        public Logger getParentLogger()
        {
            return null;
        }
    }
}
