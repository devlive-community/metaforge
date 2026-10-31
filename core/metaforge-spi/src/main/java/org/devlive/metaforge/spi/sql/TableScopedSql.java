package org.devlive.metaforge.spi.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks that a statement only works on one table: a single SELECT, INSERT, UPDATE or DELETE whose
 * every table reference (FROM, JOIN, INTO and the UPDATE target, subqueries and unions included) is
 * that table, optionally qualified with its database. Used for the SQL pane of a table's workbench.
 */
public final class TableScopedSql
{
    private static final Pattern TOKEN = Pattern.compile(
            "(--[^\\n]*|#[^\\n]*|/\\*[\\s\\S]*?\\*/)|('(?:[^'\\\\]|''|\\\\[\\s\\S])*')|(\"(?:[^\"\\\\]|\"\"|\\\\[\\s\\S])*\")|(`(?:[^`]|``)*`)|(\\s+)|([A-Za-z_][A-Za-z0-9_$]*)|([\\s\\S])");
    private static final Set<String> STATEMENTS = new HashSet<>(Arrays.asList("SELECT", "INSERT", "UPDATE", "DELETE"));
    // Words that can follow a table name without being its alias.
    private static final Set<String> CLAUSE_WORDS = new HashSet<>(Arrays.asList(
            "WHERE", "GROUP", "HAVING", "ORDER", "LIMIT", "JOIN", "INNER", "LEFT", "RIGHT", "CROSS", "NATURAL", "STRAIGHT_JOIN", "ON", "USING",
            "UNION", "SET", "VALUES", "VALUE", "SELECT", "PARTITION", "FOR", "LOCK", "WINDOW", "USE", "IGNORE", "FORCE", "AS", "INTO", "OUTER",
            "FULL", "EXCEPT", "INTERSECT", "DUPLICATE", "RETURNING", "LATERAL", "FROM", "TABLE"));
    private static final Set<String> FORBIDDEN_FUNCTIONS = new HashSet<>(Arrays.asList("LOAD_FILE"));

    private TableScopedSql()
    {
    }

    private static final class Token
    {
        private final String text;
        private final boolean word;
        private final boolean quoted;

        private Token(String text, boolean word, boolean quoted)
        {
            this.text = text;
            this.word = word;
            this.quoted = quoted;
        }

        private String upper()
        {
            return word ? text.toUpperCase(Locale.ROOT) : "";
        }

        private boolean is(String value)
        {
            return !quoted && text.equals(value);
        }

        /** Identifier text without backticks or double quotes. */
        private String name()
        {
            if (quoted) {
                char quote = text.charAt(0);
                return text.substring(1, text.length() - 1).replace(String.valueOf(quote) + quote, String.valueOf(quote));
            }
            return text;
        }
    }

    private static List<Token> tokenize(String sql)
    {
        List<Token> tokens = new ArrayList<>();
        Matcher matcher = TOKEN.matcher(sql == null ? "" : sql);
        while (matcher.find()) {
            if (matcher.group(1) != null || matcher.group(5) != null) {
                continue;
            }
            if (matcher.group(2) != null) {
                tokens.add(new Token(matcher.group(2), false, false));
            }
            else if (matcher.group(3) != null || matcher.group(4) != null) {
                tokens.add(new Token(matcher.group(3) != null ? matcher.group(3) : matcher.group(4), false, true));
            }
            else if (matcher.group(6) != null) {
                tokens.add(new Token(matcher.group(6), true, false));
            }
            else {
                tokens.add(new Token(matcher.group(7), false, false));
            }
        }
        return tokens;
    }

    /**
     * @return the reason the statement is not allowed, or empty when it only uses the table
     */
    public static Optional<String> validate(String sql, String database, String table)
    {
        List<Token> tokens = tokenize(sql);
        while (!tokens.isEmpty() && tokens.get(tokens.size() - 1).is(";")) {
            tokens.remove(tokens.size() - 1);
        }
        if (tokens.isEmpty()) {
            return Optional.of("The statement is empty");
        }
        if (tokens.stream().anyMatch(token -> token.is(";"))) {
            return Optional.of("Only one statement can run at a time");
        }
        String first = tokens.get(0).upper();
        if (!STATEMENTS.contains(first)) {
            return Optional.of("Only SELECT, INSERT, UPDATE and DELETE statements on table " + table + " are allowed");
        }
        boolean insert = "INSERT".equals(first);
        boolean seenInto = false;
        int references = 0;
        for (int index = 0; index < tokens.size(); index++) {
            Token token = tokens.get(index);
            String word = token.upper();
            if (FORBIDDEN_FUNCTIONS.contains(word) && index + 1 < tokens.size() && tokens.get(index + 1).is("(")) {
                return Optional.of("Function " + word + " is not allowed");
            }
            if ("LATERAL".equals(word)) {
                return Optional.of("LATERAL derived tables are not allowed");
            }
            boolean list;
            if ("FROM".equals(word)) {
                list = true;
            }
            else if ("JOIN".equals(word) || "STRAIGHT_JOIN".equals(word) && index > 0) {
                list = false;
            }
            else if ("UPDATE".equals(word) && index == 0) {
                list = true;
            }
            else if ("INTO".equals(word)) {
                if (!insert || seenInto) {
                    return Optional.of("SELECT ... INTO is not allowed");
                }
                seenInto = true;
                list = false;
            }
            else {
                continue;
            }
            // Read one table reference, or a comma-separated list after FROM and UPDATE.
            int cursor = index + 1;
            while (true) {
                if (cursor >= tokens.size()) {
                    return Optional.of("A table name is missing after " + word);
                }
                if (tokens.get(cursor).is("(")) {
                    // A derived table: its own FROM clauses are checked as the scan reaches them.
                    break;
                }
                Token part = tokens.get(cursor);
                if (!part.word && !part.quoted) {
                    return Optional.of("A table name is missing after " + word);
                }
                String schema = null;
                String name = part.name();
                cursor++;
                if (cursor + 1 < tokens.size() && tokens.get(cursor).is(".") && (tokens.get(cursor + 1).word || tokens.get(cursor + 1).quoted)) {
                    schema = name;
                    name = tokens.get(cursor + 1).name();
                    cursor += 2;
                }
                // After INTO a parenthesis is the column list; elsewhere it makes the name a table function.
                if (!"INTO".equals(word) && cursor < tokens.size() && tokens.get(cursor).is("(")) {
                    return Optional.of("Table functions are not allowed");
                }
                if (!name.equalsIgnoreCase(table) || (schema != null && !schema.equalsIgnoreCase(database))) {
                    return Optional.of("Only table " + table + " can be used here, found " + (schema == null ? name : schema + "." + name));
                }
                references++;
                // Optional alias: [AS] name
                if (cursor < tokens.size() && "AS".equals(tokens.get(cursor).upper())) {
                    cursor++;
                }
                if (cursor < tokens.size() && (tokens.get(cursor).quoted || tokens.get(cursor).word && !CLAUSE_WORDS.contains(tokens.get(cursor).upper()))) {
                    cursor++;
                }
                if (list && cursor < tokens.size() && tokens.get(cursor).is(",")) {
                    cursor++;
                    continue;
                }
                break;
            }
            index = cursor - 1;
        }
        if (references == 0) {
            return Optional.of("The statement must use table " + table);
        }
        return Optional.empty();
    }
}
