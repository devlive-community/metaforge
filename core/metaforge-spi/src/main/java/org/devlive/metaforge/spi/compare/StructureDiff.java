package org.devlive.metaforge.spi.compare;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Column by column comparison of two tables' structures. Columns pair up by name; a pair differs
 * when its type, nullability, default or comment differ. Source columns come first in their order,
 * then columns only the target has.
 */
public final class StructureDiff
{
    public static final String SAME = "same";
    public static final String CHANGED = "changed";
    public static final String SOURCE_ONLY = "source_only";
    public static final String TARGET_ONLY = "target_only";

    private StructureDiff()
    {
    }

    /** One column as information_schema.COLUMNS describes it. */
    public static final class Column
    {
        private final String name;
        private final String type;
        private final boolean nullable;
        private final String defaultValue;
        private final String comment;

        public Column(String name, String type, boolean nullable, String defaultValue, String comment)
        {
            this.name = name;
            this.type = type == null ? "" : type;
            this.nullable = nullable;
            this.defaultValue = defaultValue;
            this.comment = comment == null ? "" : comment;
        }

        public String getName()
        {
            return name;
        }

        public Map<String, Object> toMap()
        {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", name);
            map.put("type", type);
            map.put("nullable", nullable);
            map.put("defaultValue", defaultValue);
            map.put("comment", comment);
            return map;
        }

        /** Names of the properties that differ from another column. */
        List<String> changes(Column other)
        {
            List<String> changes = new ArrayList<>();
            if (!type.equalsIgnoreCase(other.type)) {
                changes.add("type");
            }
            if (nullable != other.nullable) {
                changes.add("nullable");
            }
            if (!Objects.equals(defaultValue, other.defaultValue)) {
                changes.add("defaultValue");
            }
            if (!comment.equals(other.comment)) {
                changes.add("comment");
            }
            return changes;
        }
    }

    /** One row per column name: status, both sides (null where missing) and the differing properties. */
    public static List<Map<String, Object>> compare(List<Column> source, List<Column> target)
    {
        Map<String, Column> targets = new LinkedHashMap<>();
        target.forEach(column -> targets.put(column.getName(), column));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Column column : source) {
            Column other = targets.remove(column.getName());
            List<String> changes = other == null ? new ArrayList<>() : column.changes(other);
            String status = other == null ? SOURCE_ONLY : changes.isEmpty() ? SAME : CHANGED;
            rows.add(row(column.getName(), status, column, other, changes));
        }
        targets.values().forEach(column -> rows.add(row(column.getName(), TARGET_ONLY, null, column, new ArrayList<>())));
        return rows;
    }

    private static Map<String, Object> row(String name, String status, Column source, Column target, List<String> changes)
    {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("name", name);
        row.put("status", status);
        row.put("source", source == null ? null : source.toMap());
        row.put("target", target == null ? null : target.toMap());
        row.put("changes", changes);
        return row;
    }
}
