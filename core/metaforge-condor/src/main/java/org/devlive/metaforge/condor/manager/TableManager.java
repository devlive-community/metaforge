package org.devlive.metaforge.condor.manager;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.condor.DataType;
import org.devlive.metaforge.condor.TableException;
import org.devlive.metaforge.condor.condition.Condition;
import org.devlive.metaforge.condor.io.BinaryMetadataCodec;
import org.devlive.metaforge.condor.io.BinaryRowCodec;
import org.devlive.metaforge.condor.metadata.ColumnDefinition;
import org.devlive.metaforge.condor.metadata.RowDefinition;
import org.devlive.metaforge.condor.metadata.TableDefinition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

@Slf4j
@SuppressFBWarnings(value = {"DLS_DEAD_LOCAL_STORE", "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"})
public class TableManager
{
    private final Path dataDir;
    private final Map<String, TableDefinition> tableMetadataCache;
    private final Map<String, ReadWriteLock> tableLocks;

    public TableManager(Path databasePath)
    {
        this.dataDir = databasePath.resolve("tables");
        this.tableMetadataCache = new HashMap<>();
        this.tableLocks = new HashMap<>();
        initializeDirectory();
    }

    private void initializeDirectory()
    {
        loadExistingTables();
    }

    private void loadExistingTables()
    {
        if (!Files.exists(dataDir)) {
            return;
        }

        log.info("Loading existing tables from {}", dataDir);
        try (Stream<Path> stream = Files.walk(dataDir, 1)) {
            stream.filter(Files::isDirectory)
                    .filter(path -> !path.equals(dataDir))
                    .filter(path -> Files.exists(path.resolve("metadata/table.meta")))
                    .forEach(tableDir -> {
                        String tableName = tableDir.getFileName().toString();
                        try {
                            TableDefinition metadata = loadTableMetadata(tableName);
                            tableMetadataCache.put(tableName, metadata);
                            tableLocks.put(tableName, new ReentrantReadWriteLock());
                        }
                        catch (IOException e) {
                            log.error("Failed to load table metadata: {}", tableName);
                        }
                    });
        }
        catch (IOException e) {
            log.error("Failed to load existing tables", e);
        }
    }

    public void createTable(TableDefinition metadata)
            throws TableException
    {
        validateTableName(metadata.getTableName());

        if (tableExists(metadata.getTableName())) {
            throw new TableException("Table '" + metadata.getTableName() + "' already exists");
        }

        try {
            saveTableMetadata(metadata);
            createTableDataFile(metadata.getTableName());

            tableMetadataCache.put(metadata.getTableName(), metadata);
            tableLocks.put(metadata.getTableName(), new ReentrantReadWriteLock());
        }
        catch (IOException e) {
            throw new TableException("Failed to create table: " + e.getMessage());
        }
    }

    public void dropTable(String tableName)
            throws TableException
    {
        if (!tableExists(tableName)) {
            throw new TableException("Table '" + tableName + "' does not exist");
        }

        ReadWriteLock lock = tableLocks.get(tableName);
        lock.writeLock().lock();
        try {
            deleteFile(dataDir.resolve(tableName));
            tableMetadataCache.remove(tableName);
            tableLocks.remove(tableName);
        }
        catch (IOException e) {
            throw new TableException("Failed to drop table: " + e.getMessage());
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    public void insert(String tableName, List<String> columnNames, List<Object> values)
            throws TableException
    {
        TableDefinition metadata = getTableMetadata(tableName);
        ReadWriteLock lock = tableLocks.get(tableName);

        lock.writeLock().lock();
        try {
            validateInsertData(metadata, columnNames, values);
            RowDefinition row = createRow(metadata, columnNames, values);
            appendRowToFile(tableName, row);
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    public void batchInsert(String tableName, List<String> columnNames, List<List<Object>> valuesList)
            throws TableException
    {
        TableDefinition metadata = getTableMetadata(tableName);
        ReadWriteLock lock = tableLocks.get(tableName);

        lock.writeLock().lock();
        try {
            for (List<Object> values : valuesList) {
                validateInsertData(metadata, columnNames, values);
            }

            List<RowDefinition> rows = new ArrayList<>();
            for (List<Object> values : valuesList) {
                rows.add(createRow(metadata, columnNames, values));
            }

            Path dataPath = getDataPath(tableName);
            try {
                BinaryRowCodec.appendRows(dataPath, rows);
            }
            catch (IOException e) {
                throw new TableException("Failed to batch insert rows: " + e.getMessage());
            }
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    public int update(String tableName, Map<String, Object> setValues, Condition whereCondition)
            throws TableException
    {
        getTableMetadata(tableName);
        ReadWriteLock lock = tableLocks.get(tableName);

        lock.writeLock().lock();
        try {
            List<RowDefinition> rows = readAllRows(tableName);
            int updatedCount = 0;

            for (RowDefinition row : rows) {
                if (whereCondition == null || whereCondition.evaluate(row)) {
                    for (Map.Entry<String, Object> entry : setValues.entrySet()) {
                        row.setValue(entry.getKey(), entry.getValue());
                    }
                    updatedCount++;
                }
            }

            if (updatedCount > 0) {
                saveAllRows(tableName, rows);
            }

            return updatedCount;
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    public int delete(String tableName, Condition whereCondition)
            throws TableException
    {
        ReadWriteLock lock = tableLocks.get(tableName);

        lock.writeLock().lock();
        try {
            List<RowDefinition> rows = readAllRows(tableName);
            List<RowDefinition> remainingRows = new ArrayList<>();
            int deletedCount = 0;

            for (RowDefinition row : rows) {
                // 无条件（null）表示删除全部行；有条件时仅删除命中的行
                boolean matched = whereCondition == null || whereCondition.evaluate(row);
                if (matched) {
                    deletedCount++;
                }
                else {
                    remainingRows.add(row);
                }
            }

            if (deletedCount > 0) {
                saveAllRows(tableName, remainingRows);
            }

            return deletedCount;
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    public List<RowDefinition> select(String tableName, List<String> columnNames, Condition whereCondition)
            throws TableException
    {
        TableDefinition metadata = getTableMetadata(tableName);
        ReadWriteLock lock = tableLocks.get(tableName);

        if (columnNames != null && !columnNames.isEmpty()) {
            for (String columnName : columnNames) {
                if (metadata.getColumn(columnName) == null) {
                    throw new TableException("Column '" + columnName + "' does not exist");
                }
            }
        }

        lock.readLock().lock();
        try {
            List<RowDefinition> rows = readAllRows(tableName);
            List<RowDefinition> result = new ArrayList<>();

            for (RowDefinition row : rows) {
                if (whereCondition == null || whereCondition.evaluate(row)) {
                    if (columnNames != null && !columnNames.isEmpty()) {
                        RowDefinition projectedRow = projectRow(row, columnNames);
                        result.add(projectedRow);
                    }
                    else {
                        result.add(row);
                    }
                }
            }

            return result;
        }
        finally {
            lock.readLock().unlock();
        }
    }

    private Path getDataPath(String tableName)
    {
        return dataDir.resolve(tableName).resolve("data").resolve("table.data");
    }

    private void appendRowToFile(String tableName, RowDefinition row)
            throws TableException
    {
        Path dataPath = getDataPath(tableName);
        try {
            BinaryRowCodec.appendRow(dataPath, row);
        }
        catch (IOException e) {
            log.error("Failed to append row to file", e);
            throw new TableException("Failed to append row to file: " + e.getMessage());
        }
    }

    private void saveAllRows(String tableName, List<RowDefinition> rows)
            throws TableException
    {
        Path dataPath = getDataPath(tableName);
        try {
            BinaryRowCodec.writeAll(dataPath, rows);
        }
        catch (IOException e) {
            throw new TableException("Failed to save rows to file: " + e.getMessage());
        }
    }

    private List<RowDefinition> readAllRows(String tableName)
            throws TableException
    {
        Path dataPath = getDataPath(tableName);
        try {
            return BinaryRowCodec.readAll(dataPath);
        }
        catch (IOException e) {
            log.error("Failed to read rows", e);
            throw new TableException("Failed to read rows: " + e.getMessage());
        }
    }

    private void validateTableName(String tableName)
            throws TableException
    {
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new TableException("Table name cannot be empty");
        }
        if (!tableName.matches("^[a-zA-Z][a-zA-Z0-9_]*$")) {
            throw new TableException("Invalid table name");
        }
        if (tableName.length() > 64) {
            throw new TableException("Table name is too long");
        }
    }

    private void validateInsertData(TableDefinition metadata, List<String> columnNames, List<Object> values)
            throws TableException
    {
        if (columnNames.size() != values.size()) {
            throw new TableException("Column count doesn't match value count");
        }

        for (int i = 0; i < columnNames.size(); i++) {
            String columnName = columnNames.get(i);
            Object value = values.get(i);
            ColumnDefinition column = metadata.getColumn(columnName);

            if (column == null) {
                throw new TableException("Column '" + columnName + "' does not exist");
            }

            if (!isValueTypeValid(value, column.getType())) {
                throw new TableException("Invalid data type for column '" + columnName + "'");
            }
        }
    }

    private boolean isValueTypeValid(Object value, DataType expectedType)
    {
        if (value == null) {
            return true;
        }

        switch (expectedType) {
            case TINYINT:
            case SMALLINT:
            case INTEGER:
            case INT:
            case BIGINT:
                return value instanceof Number;
            case FLOAT:
            case REAL:
            case DOUBLE:
            case DECIMAL:
            case NUMERIC:
                return value instanceof Number;
            case CHARACTER:
            case CHAR:
            case VARCHAR:
            case TEXT:
            case JSON:
            case XML:
                return value instanceof String;
            case BOOLEAN:
                return value instanceof Boolean;
            case BINARY:
            case VARBINARY:
            case BLOB:
                return value instanceof byte[] || value instanceof String;
            case DATE:
            case TIME:
            case TIMESTAMP:
            case DATETIME:
                return value instanceof String || value instanceof Number;
            default:
                return true;
        }
    }

    private RowDefinition createRow(TableDefinition metadata, List<String> columnNames, List<Object> values)
    {
        RowDefinition row = new RowDefinition();
        for (int i = 0; i < columnNames.size(); i++) {
            row.setValue(columnNames.get(i), values.get(i));
        }
        return row;
    }

    private RowDefinition projectRow(RowDefinition originalRow, List<String> columnNames)
    {
        RowDefinition projectedRow = new RowDefinition();
        for (String columnName : columnNames) {
            projectedRow.setValue(columnName, originalRow.getValue(columnName));
        }
        return projectedRow;
    }

    private void saveTableMetadata(TableDefinition metadata)
            throws IOException
    {
        Path metaPath = dataDir.resolve(metadata.getTableName())
                .resolve("metadata")
                .resolve("table.meta");
        BinaryMetadataCodec.write(metaPath, metadata);
    }

    private void createTableDataFile(String tableName)
            throws IOException
    {
        Path dataPath = getDataPath(tableName);
        if (!Files.exists(dataPath)) {
            Files.createDirectories(dataPath.getParent());
            Files.createFile(dataPath);
        }
    }

    private TableDefinition loadTableMetadata(String tableName)
            throws IOException
    {
        Path metaPath = dataDir.resolve(tableName)
                .resolve("metadata")
                .resolve("table.meta");
        return BinaryMetadataCodec.read(metaPath);
    }

    public String[] listTables()
    {
        return tableMetadataCache.keySet().toArray(new String[0]);
    }

    public boolean tableExists(String tableName)
    {
        return tableMetadataCache.containsKey(tableName);
    }

    public TableDefinition getTableMetadata(String tableName)
            throws TableException
    {
        TableDefinition metadata = tableMetadataCache.get(tableName);
        if (metadata == null) {
            throw new TableException("Table '" + tableName + "' does not exist");
        }
        return metadata;
    }

    private void deleteFile(Path tableDir)
            throws IOException
    {
        try (Stream<Path> stream = Files.walk(tableDir)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        }
                        catch (IOException e) {
                            log.error("Failed to delete: {}", path, e);
                        }
                    });
        }
    }
}
