package org.devlive.metaforge;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Op;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZKUtil;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.devlive.metaforge.driver.sql.ConnectionUrl;
import org.devlive.metaforge.driver.sql.SourceColumn;
import org.devlive.metaforge.driver.sql.SqlSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Three levels of znodes as databases, tables and columns; a table is one row of its columns' data. */
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"}, justification = "The source owns its ZooKeeper session")
public final class ZookeeperSource
        implements SqlSource
{
    /** ZooKeeper's own znode, never a database. */
    private static final String RESERVED = "zookeeper";

    private final ZooKeeper zookeeper;
    private final String hosts;

    private ZookeeperSource(ZooKeeper zookeeper, String hosts)
    {
        this.zookeeper = zookeeper;
        this.hosts = hosts;
    }

    static ZookeeperSource connect(ConnectionUrl url)
            throws SQLException
    {
        int sessionTimeout = Integer.parseInt(url.get("sessionTimeout", "30000"));
        long connectTimeout = Long.parseLong(url.get("connectTimeout", "10000"));
        CountDownLatch connected = new CountDownLatch(1);
        ZooKeeper zookeeper;
        try {
            zookeeper = new ZooKeeper(url.getHosts(), sessionTimeout, event -> {
                if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    connected.countDown();
                }
            });
        }
        catch (IOException error) {
            throw new SQLException("Can not reach ZooKeeper at " + url.getHosts(), error);
        }
        try {
            if (!connected.await(connectTimeout, TimeUnit.MILLISECONDS)) {
                zookeeper.close();
                throw new SQLException("ZooKeeper at " + url.getHosts() + " did not answer within " + connectTimeout + " ms");
            }
        }
        catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while connecting to ZooKeeper", error);
        }
        String user = url.get("user", null);
        String password = url.get("password", null);
        if (user != null && password != null) {
            zookeeper.addAuthInfo("digest", (user + ":" + password).getBytes(StandardCharsets.UTF_8));
        }
        return new ZookeeperSource(zookeeper, url.getHosts());
    }

    private static String path(String... names)
    {
        return "/" + String.join("/", names);
    }

    private static SQLException failure(String what, Exception error)
    {
        if (error instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        if (error instanceof KeeperException.NoNodeException) {
            return new SQLException(what + ": " + ((KeeperException) error).getPath() + " does not exist", error);
        }
        if (error instanceof KeeperException.NodeExistsException) {
            return new SQLException(what + ": " + ((KeeperException) error).getPath() + " already exists", error);
        }
        return new SQLException(what + " failed: " + error.getMessage(), error);
    }

    @Override
    public String productName()
    {
        return "ZooKeeper";
    }

    /** The server's version from the srvr command, which the official images allow; the client's otherwise. */
    @Override
    public String productVersion()
    {
        String first = hosts.split(",")[0];
        int colon = first.lastIndexOf(':');
        String host = colon < 0 ? first : first.substring(0, colon);
        int port = colon < 0 ? 2181 : Integer.parseInt(first.substring(colon + 1));
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 3000);
            socket.setSoTimeout(3000);
            OutputStream out = socket.getOutputStream();
            out.write("srvr".getBytes(StandardCharsets.US_ASCII));
            out.flush();
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String line = reader.readLine();
            if (line != null && line.startsWith("Zookeeper version:")) {
                return line.substring("Zookeeper version:".length()).trim().split("[,\\s]")[0];
            }
        }
        catch (IOException | RuntimeException ignored) {
            // srvr is not whitelisted on this server
        }
        return org.apache.zookeeper.Version.getFullVersion().split("[,\\s]")[0];
    }

    private List<String> children(String path, String what)
            throws SQLException
    {
        try {
            return zookeeper.getChildren(path, false).stream().sorted().collect(Collectors.toList());
        }
        catch (KeeperException | InterruptedException error) {
            throw failure(what, error);
        }
    }

    @Override
    public List<String> databases()
            throws SQLException
    {
        return children("/", "SHOW DATABASES").stream().filter(name -> !name.equals(RESERVED)).collect(Collectors.toList());
    }

    @Override
    public void createDatabase(String database)
            throws SQLException
    {
        create(path(database), null, "CREATE DATABASE");
    }

    @Override
    public void dropDatabase(String database)
            throws SQLException
    {
        deleteAll(path(database), "DROP DATABASE");
    }

    @Override
    public List<String> tables(String database)
            throws SQLException
    {
        return children(path(database), "SHOW TABLES");
    }

    @Override
    public void createTable(String database, String table, List<SourceColumn> columns)
            throws SQLException
    {
        List<Op> ops = new ArrayList<>();
        ops.add(Op.create(path(database, table), new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT));
        for (SourceColumn column : columns) {
            ops.add(Op.create(path(database, table, column.getName()), new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT));
        }
        multi(ops, "CREATE TABLE");
    }

    @Override
    public void dropTable(String database, String table)
            throws SQLException
    {
        deleteAll(path(database, table), "DROP TABLE");
    }

    @Override
    public List<SourceColumn> columns(String database, String table)
            throws SQLException
    {
        return children(path(database, table), "SHOW COLUMNS").stream().map(name -> SourceColumn.of(name, "VARCHAR")).collect(Collectors.toList());
    }

    @Override
    public void addColumn(String database, String table, SourceColumn column)
            throws SQLException
    {
        create(path(database, table, column.getName()), null, "ADD COLUMN");
    }

    /** Values are text whatever the type written, so a type change has nothing to convert. */
    @Override
    public void modifyColumn(String database, String table, SourceColumn column)
    {
    }

    @Override
    public void dropColumn(String database, String table, String column)
            throws SQLException
    {
        deleteAll(path(database, table, column), "DROP COLUMN");
    }

    /** Copies the value to the new znode and removes the old one in one transaction. */
    @Override
    public void renameColumn(String database, String table, String column, String name)
            throws SQLException
    {
        String from = path(database, table, column);
        try {
            if (!zookeeper.getChildren(from, false).isEmpty()) {
                throw new SQLException("RENAME COLUMN: " + from + " has children; only leaf znodes can be renamed");
            }
            Stat stat = new Stat();
            byte[] data = zookeeper.getData(from, false, stat);
            multi(List.of(Op.create(path(database, table, name), data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT),
                    Op.delete(from, stat.getVersion())), "RENAME COLUMN");
        }
        catch (KeeperException | InterruptedException error) {
            throw failure("RENAME COLUMN", error);
        }
    }

    private Map<String, Object> row(String database, String table)
            throws SQLException
    {
        Map<String, Object> row = new LinkedHashMap<>();
        for (SourceColumn column : columns(database, table)) {
            try {
                byte[] data = zookeeper.getData(path(database, table, column.getName()), false, null);
                row.put(column.getName(), data == null ? null : new String(data, StandardCharsets.UTF_8));
            }
            catch (KeeperException | InterruptedException error) {
                throw failure("SELECT", error);
            }
        }
        return row;
    }

    @Override
    public List<List<Object>> rows(String database, String table, long limit)
            throws SQLException
    {
        Map<String, Object> row = row(database, table);
        if (row.isEmpty() || limit == 0) {
            return List.of();
        }
        return List.of(new ArrayList<>(row.values()));
    }

    /** A table is a single row, so INSERT writes its column values. */
    @Override
    public int insert(String database, String table, List<String> columns, List<List<Object>> rows)
            throws SQLException
    {
        if (rows.size() != 1) {
            throw new SQLException("A ZooKeeper table holds one row; insert one row at a time");
        }
        Map<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            values.put(columns.get(i), rows.get(0).get(i));
        }
        write(database, table, values, "INSERT");
        return 1;
    }

    @Override
    public int update(String database, String table, Map<String, Object> values, Predicate<Map<String, Object>> filter)
            throws SQLException
    {
        if (!filter.test(row(database, table))) {
            return 0;
        }
        write(database, table, values, "UPDATE");
        return 1;
    }

    /** Deleting the row clears every column's value; the columns stay. */
    @Override
    public int delete(String database, String table, Predicate<Map<String, Object>> filter)
            throws SQLException
    {
        Map<String, Object> row = row(database, table);
        if (row.isEmpty() || !filter.test(row)) {
            return 0;
        }
        Map<String, Object> cleared = new LinkedHashMap<>();
        row.keySet().forEach(column -> cleared.put(column, null));
        write(database, table, cleared, "DELETE");
        return 1;
    }

    private void write(String database, String table, Map<String, Object> values, String what)
            throws SQLException
    {
        List<Op> ops = new ArrayList<>();
        values.forEach((column, value) -> ops.add(Op.setData(path(database, table, column),
                value == null ? new byte[0] : String.valueOf(value).getBytes(StandardCharsets.UTF_8), -1)));
        multi(ops, what);
    }

    private void create(String path, byte[] data, String what)
            throws SQLException
    {
        try {
            zookeeper.create(path, data == null ? new byte[0] : data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
        catch (KeeperException | InterruptedException error) {
            throw failure(what, error);
        }
    }

    private void deleteAll(String path, String what)
            throws SQLException
    {
        try {
            ZKUtil.deleteRecursive(zookeeper, path);
        }
        catch (KeeperException | InterruptedException error) {
            throw failure(what, error);
        }
    }

    private void multi(List<Op> ops, String what)
            throws SQLException
    {
        try {
            zookeeper.multi(ops);
        }
        catch (KeeperException | InterruptedException error) {
            throw failure(what, error);
        }
    }

    @Override
    public void close()
            throws SQLException
    {
        try {
            zookeeper.close();
        }
        catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while closing the ZooKeeper session", error);
        }
    }
}
