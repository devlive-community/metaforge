package org.devlive.metaforge.spi.sql;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Splits a script into statements at the semicolons that end them. Semicolons inside quotes,
 * backticks, comments and compound bodies (BEGIN ... END, CASE ... END) do not split, so a
 * CREATE TRIGGER or CREATE PROCEDURE stays one statement. The text is kept as written.
 */
public final class SqlStatements
{
    private SqlStatements()
    {
    }

    public static List<String> split(String script)
    {
        List<String> statements = new ArrayList<>();
        String sql = script == null ? "" : script;
        int length = sql.length();
        int start = 0;
        int depth = 0;
        int index = 0;
        String previousWord = "";
        while (index < length) {
            char current = sql.charAt(index);
            char next = index + 1 < length ? sql.charAt(index + 1) : '\0';
            if (current == '\'' || current == '"' || current == '`') {
                index = skipQuoted(sql, index, current);
                continue;
            }
            if ((current == '-' && next == '-') || current == '#') {
                int end = sql.indexOf('\n', index);
                index = end < 0 ? length : end + 1;
                continue;
            }
            if (current == '/' && next == '*') {
                int end = sql.indexOf("*/", index + 2);
                index = end < 0 ? length : end + 2;
                continue;
            }
            if (Character.isLetter(current) || current == '_') {
                int end = index;
                while (end < length && (Character.isLetterOrDigit(sql.charAt(end)) || sql.charAt(end) == '_' || sql.charAt(end) == '$')) {
                    end++;
                }
                String word = sql.substring(index, end).toUpperCase(Locale.ROOT);
                String following = nextWord(sql, end);
                if ("BEGIN".equals(word) && !"WORK".equals(following) && !"TRANSACTION".equals(following) && !following.isEmpty() && !"END".equals(previousWord)) {
                    depth++;
                }
                else if ("CASE".equals(word) && !"END".equals(previousWord)) {
                    depth++;
                }
                else if ("END".equals(word) && depth > 0 && !following.matches("IF|LOOP|WHILE|REPEAT")) {
                    // END closes BEGIN and CASE (also written END CASE); END IF / LOOP / WHILE / REPEAT close statements we do not count.
                    depth--;
                }
                previousWord = word;
                index = end;
                continue;
            }
            if (current == ';' && depth == 0) {
                add(statements, sql.substring(start, index));
                start = index + 1;
            }
            index++;
        }
        add(statements, sql.substring(start));
        return statements;
    }

    private static void add(List<String> statements, String statement)
    {
        if (!statement.trim().isEmpty()) {
            statements.add(statement.trim());
        }
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

    /** The next word after whitespace, upper-cased; empty when a symbol or the end comes first. */
    private static String nextWord(String sql, int index)
    {
        int cursor = index;
        while (cursor < sql.length() && Character.isWhitespace(sql.charAt(cursor))) {
            cursor++;
        }
        int end = cursor;
        while (end < sql.length() && (Character.isLetterOrDigit(sql.charAt(end)) || sql.charAt(end) == '_')) {
            end++;
        }
        return sql.substring(cursor, end).toUpperCase(Locale.ROOT);
    }
}
