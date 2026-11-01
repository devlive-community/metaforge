package org.devlive.metaforge;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.util.VersionInfo;
import org.devlive.metaforge.driver.sql.ConnectionUrl;
import org.devlive.metaforge.driver.sql.SourceColumn;
import org.devlive.metaforge.driver.sql.SqlSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Directories under a root as databases and tables; a table's rows are the files and directories in it. */
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2", "PATH_TRAVERSAL_IN"}, justification = "The source owns its file system; paths stay under root")
public final class HdfsSource
        implements SqlSource
{
    static final List<SourceColumn> COLUMNS = List.of(
            new SourceColumn("name", "VARCHAR", false, true, "Entry name"),
            new SourceColumn("type", "VARCHAR", false, false, "file or directory"),
            new SourceColumn("size", "BIGINT", false, false, "Length in bytes"),
            new SourceColumn("replication", "INTEGER", false, false, "Replication factor"),
            new SourceColumn("block_size", "BIGINT", false, false, "Block size in bytes"),
            new SourceColumn("owner", "VARCHAR", false, false, "Owner"),
            new SourceColumn("group", "VARCHAR", false, false, "Group"),
            new SourceColumn("permission", "VARCHAR", false, false, "Permission, such as rwxr-xr-x"),
            new SourceColumn("modified", "TIMESTAMP", false, false, "Modification time"),
            new SourceColumn("accessed", "TIMESTAMP", false, false, "Access time"));
    private static final Set<String> DRIVER_OPTIONS = Set.of("root", "user", "password", "httpPort");
    private static final Set<String> WRITABLE = Set.of("name", "type", "permission", "owner", "group");

    private final FileSystem fs;
    private final Path root;
    private final String namenode;
    private final int httpPort;

    private HdfsSource(FileSystem fs, Path root, String namenode, int httpPort)
    {
        this.fs = fs;
        this.root = root;
        this.namenode = namenode;
        this.httpPort = httpPort;
    }

    static HdfsSource connect(ConnectionUrl url)
            throws SQLException
    {
        Configuration configuration = new Configuration(false);
        url.getParameters().forEach((key, value) -> {
            if (!DRIVER_OPTIONS.contains(key)) {
                configuration.set(key, value);
            }
        });
        configuration.set("fs.hdfs.impl.disable.cache", "true");
        URI uri = URI.create("hdfs://" + url.getHosts());
        configuration.set("fs.defaultFS", uri.toString());
        String user = url.get("user", null);
        try {
            FileSystem fs = user == null ? FileSystem.get(uri, configuration) : FileSystem.get(uri, configuration, user);
            Path root = new Path(url.get("root", "/"));
            if (!fs.getFileStatus(root).isDirectory()) {
                fs.close();
                throw new SQLException("Root " + root + " is not a directory");
            }
            return new HdfsSource(fs, root, url.getHosts().split(",")[0].replaceFirst(":\\d+$", ""), Integer.parseInt(url.get("httpPort", "9870")));
        }
        catch (IOException error) {
            throw new SQLException("Can not reach HDFS at " + uri + ": " + error.getMessage(), error);
        }
        catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while connecting to HDFS", error);
        }
    }

    private Path path(String... names)
            throws SQLException
    {
        Path path = root;
        for (String name : names) {
            if (name.isEmpty() || name.contains("/") || name.equals("..") || name.equals(".")) {
                throw new SQLException("Invalid name " + name);
            }
            path = new Path(path, name);
        }
        return path;
    }

    private interface Call<T>
    {
        T run()
                throws IOException;
    }

    private static <T> T call(String what, Call<T> call)
            throws SQLException
    {
        try {
            return call.run();
        }
        catch (IOException error) {
            throw new SQLException(what + " failed: " + error.getMessage(), error);
        }
    }

    @Override
    public String productName()
    {
        return "HDFS";
    }

    /** The NameNode's version from its JMX page; the client's when that page is not reachable. */
    @Override
    public String productVersion()
    {
        try {
            URL jmx = new URL("http://" + namenode + ":" + httpPort + "/jmx?qry=Hadoop:service=NameNode,name=NameNodeInfo");
            HttpURLConnection connection = (HttpURLConnection) jmx.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            try (InputStream in = connection.getInputStream()) {
                Matcher matcher = Pattern.compile("\"SoftwareVersion\"\\s*:\\s*\"([^\"]+)\"").matcher(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        }
        catch (IOException | RuntimeException ignored) {
            // the NameNode's web port is not reachable from here
        }
        return VersionInfo.getVersion();
    }

    private List<String> directories(Path path, String what)
            throws SQLException
    {
        return call(what, () -> Arrays.stream(fs.listStatus(path)).filter(FileStatus::isDirectory)
                .map(status -> status.getPath().getName()).sorted().collect(Collectors.toList()));
    }

    @Override
    public List<String> databases()
            throws SQLException
    {
        return directories(root, "SHOW DATABASES");
    }

    @Override
    public void createDatabase(String database)
            throws SQLException
    {
        Path path = path(database);
        call("CREATE DATABASE", () -> fs.mkdirs(path));
    }

    @Override
    public void dropDatabase(String database)
            throws SQLException
    {
        Path path = path(database);
        call("DROP DATABASE", () -> fs.delete(path, true));
    }

    @Override
    public List<String> tables(String database)
            throws SQLException
    {
        return directories(path(database), "SHOW TABLES");
    }

    @Override
    public void createTable(String database, String table, List<SourceColumn> columns)
            throws SQLException
    {
        Set<String> fixed = COLUMNS.stream().map(SourceColumn::getName).collect(Collectors.toSet());
        for (SourceColumn column : columns) {
            if (!fixed.contains(column.getName())) {
                throw new SQLException("A directory's columns are fixed (" + String.join(", ", fixed) + "), not " + column.getName());
            }
        }
        Path path = path(database, table);
        call("CREATE TABLE", () -> fs.mkdirs(path));
    }

    @Override
    public void dropTable(String database, String table)
            throws SQLException
    {
        Path path = path(database, table);
        call("DROP TABLE", () -> fs.delete(path, true));
    }

    @Override
    public List<SourceColumn> columns(String database, String table)
    {
        return COLUMNS;
    }

    private static List<Object> row(FileStatus status)
    {
        return Arrays.asList(status.getPath().getName(), status.isDirectory() ? "directory" : "file", status.getLen(), (int) status.getReplication(),
                status.getBlockSize(), status.getOwner(), status.getGroup(), status.getPermission().toString(),
                new Timestamp(status.getModificationTime()), new Timestamp(status.getAccessTime()));
    }

    private List<FileStatus> entries(String database, String table, String what)
            throws SQLException
    {
        Path path = path(database, table);
        return call(what, () -> Arrays.stream(fs.listStatus(path)).sorted((a, b) -> a.getPath().getName().compareTo(b.getPath().getName()))
                .collect(Collectors.toList()));
    }

    @Override
    public List<List<Object>> rows(String database, String table, long limit)
            throws SQLException
    {
        return entries(database, table, "SELECT").stream().limit(limit < 0 ? Long.MAX_VALUE : limit).map(HdfsSource::row).collect(Collectors.toList());
    }

    private static Map<String, Object> named(FileStatus status)
    {
        Map<String, Object> named = new LinkedHashMap<>();
        List<Object> values = row(status);
        for (int i = 0; i < COLUMNS.size(); i++) {
            named.put(COLUMNS.get(i).getName(), values.get(i));
        }
        return named;
    }

    /** Creates an empty file (type 'file', the default) or a directory per row, then applies permission and owner. */
    @Override
    public int insert(String database, String table, List<String> columns, List<List<Object>> rows)
            throws SQLException
    {
        if (!columns.contains("name")) {
            throw new SQLException("INSERT needs the name of the entry");
        }
        for (String column : columns) {
            if (!WRITABLE.contains(column)) {
                throw new SQLException("Column " + column + " can not be written; writable are " + String.join(", ", WRITABLE));
            }
        }
        for (List<Object> row : rows) {
            Map<String, Object> values = new LinkedHashMap<>();
            for (int i = 0; i < columns.size(); i++) {
                values.put(columns.get(i), row.get(i));
            }
            Path path = path(database, table, String.valueOf(values.get("name")));
            String type = String.valueOf(values.getOrDefault("type", "file"));
            if (type.equals("directory")) {
                call("INSERT", () -> fs.mkdirs(path));
            }
            else if (type.equals("file")) {
                call("INSERT", () -> {
                    fs.create(path, false).close();
                    return null;
                });
            }
            else {
                throw new SQLException("Type is file or directory, not " + type);
            }
            attributes(path, values);
        }
        return rows.size();
    }

    private void attributes(Path path, Map<String, Object> values)
            throws SQLException
    {
        if (values.get("permission") != null) {
            String text = String.valueOf(values.get("permission"));
            FsPermission permission = text.matches("[0-7]{3,4}") ? new FsPermission(Short.parseShort(text, 8)) : FsPermission.valueOf("-" + text);
            call("Set permission", () -> {
                fs.setPermission(path, permission);
                return null;
            });
        }
        if (values.get("owner") != null || values.get("group") != null) {
            String owner = values.get("owner") == null ? null : String.valueOf(values.get("owner"));
            String group = values.get("group") == null ? null : String.valueOf(values.get("group"));
            call("Set owner", () -> {
                fs.setOwner(path, owner, group);
                return null;
            });
        }
    }

    /** SET name renames the entry; permission, owner and group change its attributes. */
    @Override
    public int update(String database, String table, Map<String, Object> values, Predicate<Map<String, Object>> filter)
            throws SQLException
    {
        for (String column : values.keySet()) {
            if (!WRITABLE.contains(column) || column.equals("type")) {
                throw new SQLException("Column " + column + " can not be changed");
            }
        }
        int count = 0;
        for (FileStatus status : entries(database, table, "UPDATE")) {
            if (!filter.test(named(status))) {
                continue;
            }
            Path path = status.getPath();
            if (values.get("name") != null) {
                Path target = path(database, table, String.valueOf(values.get("name")));
                if (!call("UPDATE", () -> fs.rename(status.getPath(), target))) {
                    throw new SQLException("Can not rename " + status.getPath().getName() + " to " + target.getName());
                }
                path = target;
            }
            attributes(path, values);
            count++;
        }
        return count;
    }

    @Override
    public int delete(String database, String table, Predicate<Map<String, Object>> filter)
            throws SQLException
    {
        List<Path> matched = new ArrayList<>();
        for (FileStatus status : entries(database, table, "DELETE")) {
            if (filter.test(named(status))) {
                matched.add(status.getPath());
            }
        }
        for (Path path : matched) {
            call("DELETE", () -> fs.delete(path, true));
        }
        return matched.size();
    }

    @Override
    public void close()
            throws SQLException
    {
        call("Close", () -> {
            fs.close();
            return null;
        });
    }
}
