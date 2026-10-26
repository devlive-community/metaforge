package org.devlive.metaforge.condor.manager;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.condor.DatabaseException;
import org.devlive.metaforge.condor.metadata.DatabaseDefinition;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

@Slf4j
@SuppressFBWarnings(value = {"RV_RETURN_VALUE_IGNORED_BAD_PRACTICE", "RV_NEGATING_RESULT_OF_COMPARETO"})
public class DatabaseManager
{
    private static final String ROOT_DIR = "data";
    private Map<String, DatabaseDefinition> databases;
    private String currentDatabase;

    public static DatabaseManager createManager()
    {
        return new DatabaseManager();
    }

    private DatabaseManager()
    {
        this.databases = new HashMap<>();
        initializeRootDirectory();
        loadExistingDatabases();
    }

    private void initializeRootDirectory()
    {
        File directory = new File(ROOT_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    private void loadExistingDatabases()
    {
        log.info("Loading existing databases from {}", ROOT_DIR);
        Path rootDir = Path.of(ROOT_DIR);
        try (Stream<Path> stream = Files.walk(rootDir)) {
            stream.filter(path -> Files.isDirectory(path)
                            && Files.exists(path.resolve("metadata/db.properties")))
                    .forEach(path -> {
                        String dbName = path.getFileName().toString();
                        log.debug("Found database: {}", dbName);
                        databases.put(dbName, new DatabaseDefinition(dbName, path));
                    });
        }
        catch (IOException e) {
            log.error("Failed to load existing databases", e);
        }
    }

    public void createDatabase(String databaseName)
            throws DatabaseException
    {
        // 验证数据库名称
        // Validate database name
        validateDatabaseName(databaseName);

        // 检查数据库是否已存在
        // Check if database already exists
        if (databases.containsKey(databaseName)) {
            log.debug("Database '{}' already exists", databaseName);
            throw new DatabaseException("Database '" + databaseName + "' already exists");
        }

        // 创建数据库目录
        // Create database directory
        try {
            log.info("Creating database directory: {}", databaseName);
            Path dbPath = Paths.get(ROOT_DIR, databaseName);
            Files.createDirectory(dbPath);

            // 创建必要的子目录
            // Create necessary subdirectories
            log.info("Creating database metadata: {}", databaseName);
            Files.createDirectory(dbPath.resolve("tables"));
            Files.createDirectory(dbPath.resolve("metadata"));

            // 创建并保存数据库配置
            // Create and save database configuration
            log.info("Creating database configuration for database: {}", databaseName);
            Properties dbConfig = new Properties();
            dbConfig.setProperty("created_time", String.valueOf(System.currentTimeMillis()));
            dbConfig.setProperty("version", "1.0");
            Path configPath = dbPath.resolve("metadata/db.properties");
            try (OutputStream os = Files.newOutputStream(configPath)) {
                dbConfig.store(os, "Database Configuration");
            }

            // 创建数据库对象并添加到管理器
            // Create database object and add to manager
            DatabaseDefinition database = new DatabaseDefinition(databaseName, dbPath);
            databases.put(databaseName, database);
            log.info("Database '{}' created successfully", databaseName);

            // 设置为当前数据库
            // Set as current database
            currentDatabase = databaseName;
        }
        catch (IOException e) {
            throw new DatabaseException("Failed to create database: " + e.getMessage());
        }
    }

    private void validateDatabaseName(String name)
            throws DatabaseException
    {
        log.info("Validating database name: {}", name);
        if (name == null || name.trim().isEmpty()) {
            throw new DatabaseException("Database name cannot be empty");
        }

        // 检查数据库名称的合法性
        // Check database name validity
        if (!name.matches("^[a-zA-Z][a-zA-Z0-9_]*$")) {
            throw new DatabaseException("Invalid database name. Database name must start with a letter and can only contain letters, numbers, and underscores");
        }

        // 检查长度限制
        // Check length limit
        if (name.length() > 64) {
            throw new DatabaseException("Database name is too long (maximum 64 characters)");
        }
    }

    public void dropDatabase(String databaseName)
            throws DatabaseException
    {
        if (!databases.containsKey(databaseName)) {
            log.info("Database '{}' does not exist", databaseName);
            throw new DatabaseException("Database '" + databaseName + "' does not exist");
        }

        try {
            // 删除数据库目录及其所有内容
            // Delete database directory and its contents
            Path dbPath = Paths.get(ROOT_DIR, databaseName);
            try (Stream<Path> stream = Files.walk(dbPath)) {
                stream.sorted((p1, p2) -> -p1.compareTo(p2))
                        .forEach(path -> {
                            try {
                                log.debug("Deleting file: {} on database: {}", path, databaseName);
                                Files.delete(path);
                            }
                            catch (IOException e) {
                                log.error("Failed to delete file: {} on database: {}", path, databaseName, e);
                            }
                        });
            }

            // 从管理器中移除数据库
            // Remove database from manager
            databases.remove(databaseName);

            // 如果删除的是当前数据库，重置当前数据库
            // Reset current database if deleted database is the current database
            if (databaseName.equals(currentDatabase)) {
                currentDatabase = null;
            }
        }
        catch (IOException e) {
            throw new DatabaseException("Failed to drop database: " + e.getMessage());
        }
    }

    public void useDatabase(String databaseName)
            throws DatabaseException
    {
        if (!databases.containsKey(databaseName)) {
            throw new DatabaseException("Database '" + databaseName + "' does not exist");
        }
        currentDatabase = databaseName;
    }

    public DatabaseDefinition getCurrentDatabase()
            throws DatabaseException
    {
        if (currentDatabase == null) {
            throw new DatabaseException("No database selected");
        }
        return databases.get(currentDatabase);
    }

    public boolean databaseExists(String databaseName)
    {
        return databases.containsKey(databaseName);
    }

    public String[] listDatabases()
    {
        return databases.keySet().toArray(new String[0]);
    }
}
