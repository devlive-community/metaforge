package org.devlive.metaforge.condor.io;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.condor.DataType;
import org.devlive.metaforge.condor.metadata.ColumnDefinition;
import org.devlive.metaforge.condor.metadata.TableDefinition;

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

@Slf4j
@SuppressFBWarnings(value = {"NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"})
public class BinaryMetadataCodec
{
    private static final byte[] MAGIC = {'C', 'M', 'T', 'A'};
    private static final short VERSION = 1;

    private BinaryMetadataCodec()
    {
    }

    public static void write(Path metaPath, TableDefinition metadata)
            throws IOException
    {
        if (!Files.exists(metaPath.getParent())) {
            Files.createDirectories(metaPath.getParent());
        }

        try (OutputStream os = Files.newOutputStream(metaPath,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                DataOutputStream dos = new DataOutputStream(os)) {
            dos.write(MAGIC);
            dos.writeShort(VERSION);

            writeString(dos, metadata.getTableName());

            List<ColumnDefinition> columns = metadata.getColumns();
            dos.writeShort(columns.size());

            for (ColumnDefinition col : columns) {
                writeString(dos, col.getName());
                writeString(dos, col.getType().name());
                dos.writeBoolean(col.isPrimaryKey());
                dos.writeBoolean(col.isNullable());
            }
        }
    }

    public static TableDefinition read(Path metaPath)
            throws IOException
    {
        try (InputStream is = Files.newInputStream(metaPath);
                DataInputStream dis = new DataInputStream(is)) {
            byte[] magic = new byte[4];
            dis.readFully(magic);
            if (magic[0] != MAGIC[0] || magic[1] != MAGIC[1]
                    || magic[2] != MAGIC[2] || magic[3] != MAGIC[3]) {
                throw new IOException("Invalid metadata file format");
            }

            short version = dis.readShort();
            if (version > VERSION) {
                throw new IOException("Unsupported metadata file version: " + version);
            }

            String tableName = readString(dis);

            int colCount = dis.readShort();
            List<ColumnDefinition> columns = new ArrayList<>();

            for (int i = 0; i < colCount; i++) {
                String name = readString(dis);
                String typeName = readString(dis);
                boolean isPrimaryKey = dis.readBoolean();
                boolean isNullable = dis.readBoolean();

                DataType type = DataType.valueOf(typeName);
                columns.add(new ColumnDefinition(name, type, isPrimaryKey, isNullable));
            }

            return new TableDefinition(tableName, columns);
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
