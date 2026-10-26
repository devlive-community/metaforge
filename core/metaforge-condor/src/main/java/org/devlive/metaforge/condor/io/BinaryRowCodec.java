package org.devlive.metaforge.condor.io;

import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.condor.metadata.RowDefinition;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class BinaryRowCodec
{
    private static final byte[] MAGIC = {'C', 'D', 'O', 'R'};
    private static final short VERSION = 1;

    private static final byte TYPE_NULL = 0;
    private static final byte TYPE_INTEGER = 1;
    private static final byte TYPE_LONG = 2;
    private static final byte TYPE_DOUBLE = 3;
    private static final byte TYPE_BOOLEAN = 4;
    private static final byte TYPE_STRING = 5;
    private static final byte TYPE_FLOAT = 6;

    private BinaryRowCodec()
    {
    }

    public static void writeAll(Path dataPath, List<RowDefinition> rows)
            throws IOException
    {
        try (OutputStream os = Files.newOutputStream(dataPath,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                DataOutputStream dos = new DataOutputStream(os)) {
            dos.write(MAGIC);
            dos.writeShort(VERSION);
            dos.writeInt(rows.size());

            for (RowDefinition row : rows) {
                writeRow(dos, row);
            }
        }
    }

    public static void appendRows(Path dataPath, List<RowDefinition> rows)
            throws IOException
    {
        if (!Files.exists(dataPath) || Files.size(dataPath) == 0) {
            writeAll(dataPath, rows);
            return;
        }

        List<RowDefinition> existing = readAll(dataPath);
        existing.addAll(rows);
        writeAll(dataPath, existing);
    }

    public static void appendRow(Path dataPath, RowDefinition row)
            throws IOException
    {
        List<RowDefinition> rows = new ArrayList<>();
        rows.add(row);
        appendRows(dataPath, rows);
    }

    public static List<RowDefinition> readAll(Path dataPath)
            throws IOException
    {
        List<RowDefinition> rows = new ArrayList<>();

        if (!Files.exists(dataPath) || Files.size(dataPath) == 0) {
            return rows;
        }

        try (InputStream is = Files.newInputStream(dataPath);
                DataInputStream dis = new DataInputStream(is)) {
            byte[] magic = new byte[4];
            dis.readFully(magic);
            if (magic[0] != MAGIC[0] || magic[1] != MAGIC[1]
                    || magic[2] != MAGIC[2] || magic[3] != MAGIC[3]) {
                throw new IOException("Invalid data file format");
            }

            short version = dis.readShort();
            if (version > VERSION) {
                throw new IOException("Unsupported data file version: " + version);
            }

            int rowCount = dis.readInt();
            for (int i = 0; i < rowCount; i++) {
                rows.add(readRow(dis));
            }
        }

        return rows;
    }

    private static void writeRow(DataOutputStream dos, RowDefinition row)
            throws IOException
    {
        Map<String, Object> values = row.getValues();
        dos.writeShort(values.size());

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            writeString(dos, entry.getKey());
            writeValue(dos, entry.getValue());
        }
    }

    private static RowDefinition readRow(DataInputStream dis)
            throws IOException
    {
        RowDefinition row = new RowDefinition();
        int colCount = dis.readShort();

        for (int i = 0; i < colCount; i++) {
            String name = readString(dis);
            Object value = readValue(dis);
            row.setValue(name, value);
        }

        return row;
    }

    private static void writeValue(DataOutputStream dos, Object value)
            throws IOException
    {
        if (value == null) {
            dos.writeByte(TYPE_NULL);
        }
        else if (value instanceof Integer) {
            dos.writeByte(TYPE_INTEGER);
            dos.writeInt((Integer) value);
        }
        else if (value instanceof Long) {
            dos.writeByte(TYPE_LONG);
            dos.writeLong((Long) value);
        }
        else if (value instanceof Double) {
            dos.writeByte(TYPE_DOUBLE);
            dos.writeDouble((Double) value);
        }
        else if (value instanceof Float) {
            dos.writeByte(TYPE_FLOAT);
            dos.writeFloat((Float) value);
        }
        else if (value instanceof Boolean) {
            dos.writeByte(TYPE_BOOLEAN);
            dos.writeBoolean((Boolean) value);
        }
        else {
            dos.writeByte(TYPE_STRING);
            writeString(dos, value.toString());
        }
    }

    private static Object readValue(DataInputStream dis)
            throws IOException
    {
        byte type = dis.readByte();
        switch (type) {
            case TYPE_NULL:
                return null;
            case TYPE_INTEGER:
                return dis.readInt();
            case TYPE_LONG:
                return dis.readLong();
            case TYPE_DOUBLE:
                return dis.readDouble();
            case TYPE_FLOAT:
                return dis.readFloat();
            case TYPE_BOOLEAN:
                return dis.readBoolean();
            case TYPE_STRING:
                return readString(dis);
            default:
                throw new IOException("Unknown value type: " + type);
        }
    }

    private static void writeString(DataOutputStream dos, String str)
            throws IOException
    {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        dos.writeShort(bytes.length);
        dos.write(bytes);
    }

    private static String readString(DataInputStream dis)
            throws IOException
    {
        int len = dis.readShort();
        byte[] bytes = new byte[len];
        dis.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
