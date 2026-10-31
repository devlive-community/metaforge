package org.devlive.metaforge.driver.sql;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** A source keeping databases, tables and rows in maps, for testing the driver layer. */
class MemorySource
        implements SqlSource
{
    final Map<String, Map<String, List<SourceColumn>>> columns = new LinkedHashMap<>();
    final Map<String, List<Map<String, Object>>> rows = new LinkedHashMap<>();
    final Map<String, String> options = new LinkedHashMap<>();

    @Override
    public String productName()
    {
        return "Memory";
    }

    @Override
    public String productVersion()
    {
        return "1.2.3";
    }

    @Override
    public List<String> databases()
    {
        return new ArrayList<>(columns.keySet());
    }

    @Override
    public void createDatabase(String database)
    {
        columns.put(database, new LinkedHashMap<>());
    }

    @Override
    public void dropDatabase(String database)
    {
        columns.remove(database);
    }

    @Override
    public List<String> tables(String database)
            throws SQLException
    {
        if (!columns.containsKey(database)) {
            throw new SQLException("Unknown database " + database);
        }
        return new ArrayList<>(columns.get(database).keySet());
    }

    @Override
    public void createTable(String database, String table, List<SourceColumn> definition)
    {
        columns.get(database).put(table, new ArrayList<>(definition));
        rows.put(database + "." + table, new ArrayList<>());
    }

    @Override
    public void createTable(String database, String table, List<SourceColumn> definition, Map<String, String> with)
    {
        options.putAll(with);
        createTable(database, table, definition);
    }

    @Override
    public void dropTable(String database, String table)
    {
        columns.get(database).remove(table);
    }

    @Override
    public List<SourceColumn> columns(String database, String table)
    {
        return columns.get(database).get(table);
    }

    @Override
    public void addColumn(String database, String table, SourceColumn column)
    {
        columns.get(database).get(table).add(column);
    }

    @Override
    public void dropColumn(String database, String table, String column)
    {
        columns.get(database).get(table).removeIf(item -> item.getName().equals(column));
        rows.get(database + "." + table).forEach(row -> row.remove(column));
    }

    @Override
    public void renameColumn(String database, String table, String column, String name)
    {
        List<SourceColumn> list = columns.get(database).get(table);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getName().equals(column)) {
                list.set(i, SourceColumn.of(name, list.get(i).getType()));
            }
        }
        rows.get(database + "." + table).forEach(row -> row.put(name, row.remove(column)));
    }

    @Override
    public List<List<Object>> rows(String database, String table, long limit)
    {
        List<List<Object>> values = new ArrayList<>();
        for (Map<String, Object> row : rows.get(database + "." + table)) {
            List<Object> value = new ArrayList<>();
            columns(database, table).forEach(column -> value.add(row.get(column.getName())));
            values.add(value);
        }
        return values;
    }

    @Override
    public int insert(String database, String table, List<String> names, List<List<Object>> values)
    {
        for (List<Object> value : values) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 0; i < names.size(); i++) {
                row.put(names.get(i), value.get(i));
            }
            rows.get(database + "." + table).add(row);
        }
        return values.size();
    }

    @Override
    public int update(String database, String table, Map<String, Object> values, Predicate<Map<String, Object>> filter)
    {
        int count = 0;
        for (Map<String, Object> row : rows.get(database + "." + table)) {
            if (filter.test(row)) {
                row.putAll(values);
                count++;
            }
        }
        return count;
    }

    @Override
    public int delete(String database, String table, Predicate<Map<String, Object>> filter)
    {
        List<Map<String, Object>> list = rows.get(database + "." + table);
        int before = list.size();
        list.removeIf(filter);
        return before - list.size();
    }

    @Override
    public void close()
    {
    }
}
