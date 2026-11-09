package org.devlive.metaforge.service.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Conservative SQL subset for unattended chart refresh. The database also enforces a read-only transaction. */
final class ReadOnlyChartSql
{
    private static final Set<String> BLOCKED = Set.of("INTO", "FOR", "LOCK", "OUTFILE", "DUMPFILE", "PROCEDURE", "OPERATOR");
    private static final Set<String> CALLS = Set.of(
            "SELECT", "FROM", "AS", "WHERE", "HAVING", "ON", "AND", "OR", "NOT", "IN", "EXISTS", "BY",
            "WHEN", "THEN", "ELSE", "OVER", "FILTER", "UNION", "INTERSECT", "EXCEPT", "JOIN",
            "COUNT", "SUM", "MIN", "MAX", "AVG", "COALESCE", "NULLIF", "ABS", "ROUND", "CEIL", "FLOOR",
            "LOWER", "UPPER", "LENGTH", "CHAR_LENGTH", "CONCAT", "TRIM", "SUBSTRING", "REPLACE",
            "DATE", "DATE_TRUNC", "DATE_FORMAT", "EXTRACT", "CAST", "DECIMAL", "NUMERIC", "CHAR", "VARCHAR",
            "ROW_NUMBER", "RANK", "DENSE_RANK", "LAG", "LEAD");

    private ReadOnlyChartSql() {}

    static boolean accepts(String sql)
    {
        if (sql == null || sql.length() > 32768 || sql.indexOf('\\') >= 0 || sql.indexOf(0) >= 0) {
            return false;
        }

        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < sql.length(); ) {
            char ch = sql.charAt(i);
            char next = i + 1 < sql.length() ? sql.charAt(i + 1) : 0;
            if (Character.isWhitespace(ch)) {
                i++;
            }
            else if (ch == ';') {
                if (!sql.substring(i + 1).isBlank()) {
                    return false;
                }
                break;
            }
            else if (ch == '\'' || ch == '"' || ch == '`') {
                boolean closed = false;
                for (i++; i < sql.length(); i++) {
                    if (sql.charAt(i) == ch) {
                        if (i + 1 < sql.length() && sql.charAt(i + 1) == ch) {
                            i++;
                            continue;
                        }
                        i++;
                        closed = true;
                        break;
                    }
                }
                if (!closed) {
                    return false;
                }
                tokens.add(ch == '\'' ? "literal" : "identifier");
            }
            else if (ch == '#' || ch == '$' || ch == '@' || (ch == '-' && next == '-') || (ch == '/' && next == '*')) {
                return false;
            }
            else if (Character.isLetter(ch) || ch == '_') {
                int start = i++;
                while (i < sql.length() && (Character.isLetterOrDigit(sql.charAt(i)) || sql.charAt(i) == '_')) {
                    i++;
                }
                String word = sql.substring(start, i).toUpperCase(Locale.ROOT);
                if (BLOCKED.contains(word)) {
                    return false;
                }
                tokens.add(word);
            }
            else {
                tokens.add(String.valueOf(ch));
                i++;
            }
        }

        if (tokens.size() < 2 || !"SELECT".equals(tokens.get(0))) {
            return false;
        }
        for (int i = 1; i + 1 < tokens.size(); i++) {
            String word = tokens.get(i);
            if ("(".equals(tokens.get(i + 1)) && Character.isLetter(word.charAt(0))
                    && (!CALLS.contains(word) || ".".equals(tokens.get(i - 1)))) {
                return false;
            }
            if ("(".equals(tokens.get(i + 1)) && word.charAt(0) == '_') {
                return false;
            }
        }
        return true;
    }
}
