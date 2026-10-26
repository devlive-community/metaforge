package org.devlive.metaforge.condor.io;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.condor.DataType;
import org.devlive.metaforge.condor.metadata.ColumnDefinition;
import org.devlive.metaforge.condor.metadata.RowDefinition;
import org.devlive.metaforge.condor.metadata.TableDefinition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@Slf4j
@SuppressFBWarnings(value = {"CNT_ROUGH_CONSTANT_VALUE"})
public class BinaryCodecTest
{
    private Path tempDir;

    @Before
    public void setUp()
            throws IOException
    {
        tempDir = Files.createTempDirectory("condor_test_");
    }

    @After
    public void tearDown()
            throws IOException
    {
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        }
                        catch (IOException ignored) {
                        }
                    });
        }
    }

    @Test
    public void testRowCodecRoundTrip()
            throws IOException
    {
        Path dataPath = tempDir.resolve("test.data");

        List<RowDefinition> rows = new ArrayList<>();

        RowDefinition row1 = new RowDefinition();
        row1.setValue("id", 1);
        row1.setValue("name", "Alice");
        row1.setValue("score", 95.5);
        row1.setValue("active", true);
        rows.add(row1);

        RowDefinition row2 = new RowDefinition();
        row2.setValue("id", 2);
        row2.setValue("name", "Bob");
        row2.setValue("score", 87.3);
        row2.setValue("active", false);
        rows.add(row2);

        BinaryRowCodec.writeAll(dataPath, rows);

        List<RowDefinition> loaded = BinaryRowCodec.readAll(dataPath);
        assertEquals(2, loaded.size());

        assertEquals(1, loaded.get(0).getValue("id"));
        assertEquals("Alice", loaded.get(0).getValue("name"));
        assertEquals(95.5, (double) loaded.get(0).getValue("score"), 0.01);
        assertEquals(true, loaded.get(0).getValue("active"));

        assertEquals(2, loaded.get(1).getValue("id"));
        assertEquals("Bob", loaded.get(1).getValue("name"));
    }

    @Test
    public void testRowCodecNullValues()
            throws IOException
    {
        Path dataPath = tempDir.resolve("null.data");

        List<RowDefinition> rows = new ArrayList<>();
        RowDefinition row = new RowDefinition();
        row.setValue("id", 1);
        row.setValue("name", null);
        rows.add(row);

        BinaryRowCodec.writeAll(dataPath, rows);

        List<RowDefinition> loaded = BinaryRowCodec.readAll(dataPath);
        assertEquals(1, loaded.size());
        assertEquals(1, loaded.get(0).getValue("id"));
        assertNull(loaded.get(0).getValue("name"));
    }

    @Test
    public void testRowCodecAppend()
            throws IOException
    {
        Path dataPath = tempDir.resolve("append.data");

        RowDefinition row1 = new RowDefinition();
        row1.setValue("id", 1);
        BinaryRowCodec.appendRow(dataPath, row1);

        RowDefinition row2 = new RowDefinition();
        row2.setValue("id", 2);
        BinaryRowCodec.appendRow(dataPath, row2);

        List<RowDefinition> loaded = BinaryRowCodec.readAll(dataPath);
        assertEquals(2, loaded.size());
        assertEquals(1, loaded.get(0).getValue("id"));
        assertEquals(2, loaded.get(1).getValue("id"));
    }

    @Test
    public void testRowCodecAppendBatch()
            throws IOException
    {
        Path dataPath = tempDir.resolve("batch.data");

        RowDefinition row1 = new RowDefinition();
        row1.setValue("id", 1);
        BinaryRowCodec.appendRow(dataPath, row1);

        List<RowDefinition> batch = new ArrayList<>();
        RowDefinition row2 = new RowDefinition();
        row2.setValue("id", 2);
        batch.add(row2);
        RowDefinition row3 = new RowDefinition();
        row3.setValue("id", 3);
        batch.add(row3);
        BinaryRowCodec.appendRows(dataPath, batch);

        List<RowDefinition> loaded = BinaryRowCodec.readAll(dataPath);
        assertEquals(3, loaded.size());
    }

    @Test
    public void testRowCodecEmptyFile()
            throws IOException
    {
        Path dataPath = tempDir.resolve("empty.data");
        Files.createFile(dataPath);

        List<RowDefinition> loaded = BinaryRowCodec.readAll(dataPath);
        assertTrue(loaded.isEmpty());
    }

    @Test
    public void testRowCodecNonExistentFile()
            throws IOException
    {
        Path dataPath = tempDir.resolve("nonexistent.data");

        List<RowDefinition> loaded = BinaryRowCodec.readAll(dataPath);
        assertTrue(loaded.isEmpty());
    }

    @Test
    public void testRowCodecAllTypes()
            throws IOException
    {
        Path dataPath = tempDir.resolve("types.data");

        List<RowDefinition> rows = new ArrayList<>();
        RowDefinition row = new RowDefinition();
        row.setValue("int_val", 42);
        row.setValue("long_val", 123456789L);
        row.setValue("double_val", 3.14);
        row.setValue("float_val", 2.71f);
        row.setValue("bool_val", true);
        row.setValue("string_val", "hello world");
        row.setValue("null_val", null);
        rows.add(row);

        BinaryRowCodec.writeAll(dataPath, rows);

        List<RowDefinition> loaded = BinaryRowCodec.readAll(dataPath);
        assertEquals(1, loaded.size());
        RowDefinition loadedRow = loaded.get(0);

        assertEquals(42, loadedRow.getValue("int_val"));
        assertEquals(123456789L, loadedRow.getValue("long_val"));
        assertEquals(3.14, (double) loadedRow.getValue("double_val"), 0.001);
        assertEquals(2.71f, (float) loadedRow.getValue("float_val"), 0.001);
        assertEquals(true, loadedRow.getValue("bool_val"));
        assertEquals("hello world", loadedRow.getValue("string_val"));
        assertNull(loadedRow.getValue("null_val"));
    }

    @Test
    public void testMetadataCodecRoundTrip()
            throws IOException
    {
        Path metaPath = tempDir.resolve("table.meta");

        List<ColumnDefinition> columns = Arrays.asList(
                new ColumnDefinition("id", DataType.INTEGER, true, false),
                new ColumnDefinition("name", DataType.VARCHAR, false, true),
                new ColumnDefinition("score", DataType.DOUBLE, false, true),
                new ColumnDefinition("active", DataType.BOOLEAN, false, true)
        );
        TableDefinition original = new TableDefinition("test_table", columns);

        BinaryMetadataCodec.write(metaPath, original);

        TableDefinition loaded = BinaryMetadataCodec.read(metaPath);
        assertEquals("test_table", loaded.getTableName());
        assertEquals(4, loaded.getColumns().size());

        ColumnDefinition idCol = loaded.getColumn("id");
        assertEquals("id", idCol.getName());
        assertEquals(DataType.INTEGER, idCol.getType());
        assertTrue(idCol.isPrimaryKey());
        assertFalse(idCol.isNullable());

        ColumnDefinition nameCol = loaded.getColumn("name");
        assertEquals("name", nameCol.getName());
        assertEquals(DataType.VARCHAR, nameCol.getType());
        assertFalse(nameCol.isPrimaryKey());
        assertTrue(nameCol.isNullable());
    }

    @Test
    public void testMetadataCodecAllDataTypes()
            throws IOException
    {
        Path metaPath = tempDir.resolve("alltypes.meta");

        List<ColumnDefinition> columns = Arrays.asList(
                new ColumnDefinition("c1", DataType.INTEGER, false, true),
                new ColumnDefinition("c2", DataType.BIGINT, false, true),
                new ColumnDefinition("c3", DataType.VARCHAR, false, true),
                new ColumnDefinition("c4", DataType.TEXT, false, true),
                new ColumnDefinition("c5", DataType.DOUBLE, false, true),
                new ColumnDefinition("c6", DataType.FLOAT, false, true),
                new ColumnDefinition("c7", DataType.BOOLEAN, false, true),
                new ColumnDefinition("c8", DataType.TIMESTAMP, false, true)
        );
        TableDefinition original = new TableDefinition("type_table", columns);

        BinaryMetadataCodec.write(metaPath, original);
        TableDefinition loaded = BinaryMetadataCodec.read(metaPath);

        assertEquals(8, loaded.getColumns().size());
        assertEquals(DataType.BIGINT, loaded.getColumn("c2").getType());
        assertEquals(DataType.TEXT, loaded.getColumn("c4").getType());
        assertEquals(DataType.FLOAT, loaded.getColumn("c6").getType());
        assertEquals(DataType.TIMESTAMP, loaded.getColumn("c8").getType());
    }

    @Test
    public void testMetadataCodecCreateDirectory()
            throws IOException
    {
        Path metaPath = tempDir.resolve("sub").resolve("dir").resolve("table.meta");

        List<ColumnDefinition> columns = Arrays.asList(
                new ColumnDefinition("id", DataType.INTEGER, true, false)
        );
        TableDefinition original = new TableDefinition("t", columns);

        BinaryMetadataCodec.write(metaPath, original);
        assertTrue(Files.exists(metaPath));

        TableDefinition loaded = BinaryMetadataCodec.read(metaPath);
        assertEquals("t", loaded.getTableName());
    }
}
