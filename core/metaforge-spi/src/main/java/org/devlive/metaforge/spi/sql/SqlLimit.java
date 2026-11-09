package org.devlive.metaforge.spi.sql;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Bounds a query in the SQL itself: a single SELECT (or WITH ... SELECT) without a row clause of its
 * own gets a {@code LIMIT} appended, so the database stops after that many rows. Statements that
 * already limit, page or lock their rows, write them somewhere, or end with an engine clause that
 * must follow LIMIT are left as written; the JDBC row cap still bounds them.
 */
public final class SqlLimit
{
    /** Top-level words after which an appended LIMIT is invalid or changes the meaning. */
    private static final Set<String> KEEP = Set.of(
            // the statement limits or pages its rows already
            "LIMIT", "FETCH", "OFFSET", "TOP",
            // SELECT ... INTO, FOR UPDATE, LOCK IN SHARE MODE
            "INTO", "FOR", "LOCK",
            // ClickHouse clauses that follow LIMIT
            "SETTINGS", "FORMAT",
            // WITH ... INSERT / UPDATE / DELETE / MERGE
            "INSERT", "UPDATE", "DELETE", "MERGE");

    private SqlLimit()
    {
    }

    public static Optional<String> append(String sql, int limit)
    {
        List<String> statements = SqlStatements.split(sql);
        if (limit < 1 || statements.size() != 1) {
            return Optional.empty();
        }
        String statement = statements.get(0);
        String first = null;
        int depth = 0;
        int index = 0;
        int length = statement.length();
        while (index < length) {
            char current = statement.charAt(index);
            char next = index + 1 < length ? statement.charAt(index + 1) : '\0';
            if (current == '\'' || current == '"' || current == '`') {
                index = skipQuoted(statement, index, current);
                continue;
            }
            if ((current == '-' && next == '-') || current == '#') {
                int end = statement.indexOf('\n', index);
                index = end < 0 ? length : end + 1;
                continue;
            }
            if (current == '/' && next == '*') {
                int end = statement.indexOf("*/", index + 2);
                index = end < 0 ? length : end + 2;
                continue;
            }
            if (current == '(') {
                depth++;
            }
            else if (current == ')') {
                depth--;
            }
            else if (Character.isLetter(current) || current == '_') {
                int end = index;
                while (end < length && (Character.isLetterOrDigit(statement.charAt(end)) || statement.charAt(end) == '_' || statement.charAt(end) == '$')) {
                    end++;
                }
                String word = statement.substring(index, end).toUpperCase(Locale.ROOT);
                if (first == null) {
                    first = word;
                    if (!"SELECT".equals(word) && !"WITH".equals(word)) {
                        return Optional.empty();
                    }
                }
                else if (depth == 0 && KEEP.contains(word)) {
                    return Optional.empty();
                }
                index = end;
                continue;
            }
            index++;
        }
        if (first == null || depth != 0) {
            return Optional.empty();
        }
        // On its own line, so a trailing line comment cannot swallow it.
        return Optional.of(statement + "\nLIMIT " + limit);
    }

    /** Index just after the closing quote; doubled quotes and backslash escapes stay inside. */
    private static int skipQuoted(String sql, int index, char quote)
    {
        int cursor = index + 1;
        while (cursor < sql.length()) {
            char current = sql.charAt(cursor);
            if (current == '\\' && quote != '`') {
                cursor += 2;
                continue;
            }
            if (current == quote) {
                if (cursor + 1 < sql.length() && sql.charAt(cursor + 1) == quote) {
                    cursor += 2;
                    continue;
                }
                return cursor + 1;
            }
            cursor++;
        }
        return sql.length();
    }
}
