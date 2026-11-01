package org.devlive.metaforge.driver.sql;

import java.util.ArrayList;
import java.util.List;

/** Identifiers and literals as the parser hands them over, quotes included. */
public final class Names
{
    private Names()
    {
    }

    /** `name`, "name" and [name] without their quotes. */
    public static String unquote(String name)
    {
        if (name == null || name.length() < 2) {
            return name;
        }
        char first = name.charAt(0);
        char last = name.charAt(name.length() - 1);
        if ((first == '`' && last == '`') || (first == '"' && last == '"') || (first == '[' && last == ']')) {
            return name.substring(1, name.length() - 1);
        }
        return name;
    }

    /** A string literal's text, quotes removed and doubled quotes undone; other values unchanged. */
    public static Object literal(Object value)
    {
        if (!(value instanceof String)) {
            return value;
        }
        String text = (String) value;
        if (text.length() >= 2 && (text.charAt(0) == '\'' || text.charAt(0) == '"') && text.charAt(text.length() - 1) == text.charAt(0)) {
            String quote = String.valueOf(text.charAt(0));
            return text.substring(1, text.length() - 1).replace(quote + quote, quote).replace("\\" + quote, quote);
        }
        return text;
    }

    /** [database, table] of db.table or table, the database defaulting to the connection's. */
    public static String[] table(String name, String database)
            throws java.sql.SQLException
    {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        char quote = 0;
        for (char c : name.toCharArray()) {
            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                }
                current.append(c);
            }
            else if (c == '`' || c == '"') {
                quote = c;
                current.append(c);
            }
            else if (c == '.') {
                parts.add(unquote(current.toString()));
                current.setLength(0);
            }
            else {
                current.append(c);
            }
        }
        parts.add(unquote(current.toString()));
        if (parts.size() == 1) {
            if (database == null || database.isEmpty()) {
                throw new java.sql.SQLException("No database selected for table " + parts.get(0) + "; write database.table or USE a database");
            }
            return new String[] {database, parts.get(0)};
        }
        if (parts.size() == 2) {
            return new String[] {parts.get(0), parts.get(1)};
        }
        throw new java.sql.SQLException("Invalid table name " + name);
    }
}
