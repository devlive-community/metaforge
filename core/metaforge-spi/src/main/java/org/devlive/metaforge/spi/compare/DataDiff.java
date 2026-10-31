package org.devlive.metaforge.spi.compare;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Row by row comparison of two result sets that share their columns. Rows pair up by the key
 * columns; a pair differs when any column value differs (compared as text, NULL apart). Source rows
 * come first in their order, then rows only the target has.
 */
public final class DataDiff
{
    public static final String SAME = "same";
    public static final String CHANGED = "changed";
    public static final String SOURCE_ONLY = "source_only";
    public static final String TARGET_ONLY = "target_only";

    private DataDiff()
    {
    }

    /** One differing row: its type, key values, both sides (null where missing) and the differing columns. */
    public static final class Row
    {
        private final String type;
        private final List<Object> key;
        private final List<Object> source;
        private final List<Object> target;
        private final List<String> changed;

        Row(String type, List<Object> key, List<Object> source, List<Object> target, List<String> changed)
        {
            this.type = type;
            this.key = key;
            this.source = source;
            this.target = target;
            this.changed = changed;
        }

        public String getType()
        {
            return type;
        }

        public List<Object> getKey()
        {
            return new ArrayList<>(key);
        }

        public List<Object> getSource()
        {
            return source == null ? null : new ArrayList<>(source);
        }

        public List<Object> getTarget()
        {
            return target == null ? null : new ArrayList<>(target);
        }

        public List<String> getChanged()
        {
            return new ArrayList<>(changed);
        }
    }

    public static final class Result
    {
        private final List<Row> rows;
        private final long same;

        Result(List<Row> rows, long same)
        {
            this.rows = rows;
            this.same = same;
        }

        public List<Row> getRows()
        {
            return new ArrayList<>(rows);
        }

        public long getSame()
        {
            return same;
        }

        public long count(String type)
        {
            return rows.stream().filter(row -> row.type.equals(type)).count();
        }
    }

    /**
     * @param columns the compared column names, in the order of each row's values
     * @param keys the key columns, a subset of {@code columns}
     * @param keepSame whether identical pairs are listed too (as {@link #SAME}) or only counted
     */
    public static Result compare(List<String> columns, List<String> keys, List<List<Object>> source, List<List<Object>> target, boolean keepSame)
    {
        List<Integer> keyIndexes = new ArrayList<>();
        keys.forEach(key -> {
            int index = columns.indexOf(key);
            if (index < 0) {
                throw new IllegalArgumentException("Key column " + key + " is not compared");
            }
            keyIndexes.add(index);
        });
        Map<List<String>, List<Object>> targets = new LinkedHashMap<>();
        target.forEach(row -> targets.put(identity(row, keyIndexes), row));
        List<Row> rows = new ArrayList<>();
        long same = 0;
        for (List<Object> row : source) {
            List<Object> other = targets.remove(identity(row, keyIndexes));
            if (other == null) {
                rows.add(new Row(SOURCE_ONLY, key(row, keyIndexes), row, null, new ArrayList<>()));
                continue;
            }
            List<String> changed = new ArrayList<>();
            for (int index = 0; index < columns.size(); index++) {
                if (!Objects.equals(text(row.get(index)), text(other.get(index)))) {
                    changed.add(columns.get(index));
                }
            }
            if (changed.isEmpty()) {
                same++;
                if (keepSame) {
                    rows.add(new Row(SAME, key(row, keyIndexes), row, other, changed));
                }
            }
            else {
                rows.add(new Row(CHANGED, key(row, keyIndexes), row, other, changed));
            }
        }
        targets.values().forEach(row -> rows.add(new Row(TARGET_ONLY, key(row, keyIndexes), null, row, new ArrayList<>())));
        return new Result(rows, same);
    }

    private static List<String> identity(List<Object> row, List<Integer> keyIndexes)
    {
        List<String> identity = new ArrayList<>();
        keyIndexes.forEach(index -> identity.add(text(row.get(index))));
        return identity;
    }

    private static List<Object> key(List<Object> row, List<Integer> keyIndexes)
    {
        List<Object> key = new ArrayList<>();
        keyIndexes.forEach(index -> key.add(row.get(index)));
        return key;
    }

    private static String text(Object value)
    {
        return value == null ? null : String.valueOf(value);
    }
}
