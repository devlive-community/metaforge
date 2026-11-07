package org.devlive.metaforge.driver;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.client.protocol.RedisCommand;
import org.redisson.client.protocol.decoder.ObjectListReplayDecoder;
import org.redisson.command.CommandAsyncExecutor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Plain Redis commands ({@code GET key}, {@code HGETALL hash}, {@code KEYS *}), as the released
 * driver ran them: everything that is not the SQL subset ({@code SELECT ... / SHOW ...}) is sent to
 * Redis, and the reply comes back as one RESULTS column with a row per value.
 */
public final class RedisCommands
{
    public static final String COLUMN = "RESULTS";
    /** Redis' own SELECT switches the database: {@code SELECT 2}. */
    private static final Pattern SELECT_DATABASE = Pattern.compile("(?is)^\\s*select\\s+\\d+\\s*;?\\s*$");

    private RedisCommands()
    {}

    public static boolean isCommand(String sql)
    {
        List<String> words = tokenize(sql);
        if (words.isEmpty()) {
            return false;
        }
        String first = words.get(0).toUpperCase(Locale.ROOT);
        return !("SELECT".equals(first) || "SHOW".equals(first)) || SELECT_DATABASE.matcher(sql).matches();
    }

    /** Splits a command into words; single or double quotes keep spaces, a backslash escapes the next character. */
    public static List<String> tokenize(String sql)
    {
        List<String> words = new ArrayList<>();
        if (sql == null) {
            return words;
        }
        String text = sql.trim();
        if (text.endsWith(";")) {
            text = text.substring(0, text.length() - 1);
        }
        StringBuilder word = new StringBuilder();
        boolean inWord = false;
        char quote = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length()) {
                word.append(text.charAt(++i));
                inWord = true;
            }
            else if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                }
                else {
                    word.append(c);
                }
            }
            else if (c == '"' || c == '\'') {
                quote = c;
                inWord = true;
            }
            else if (Character.isWhitespace(c)) {
                if (inWord) {
                    words.add(word.toString());
                    word.setLength(0);
                    inWord = false;
                }
            }
            else {
                word.append(c);
                inWord = true;
            }
        }
        if (inWord) {
            words.add(word.toString());
        }
        return words;
    }

    /** Runs a command; in a cluster it goes to the node of its first argument, the key of most commands. */
    public static List<Map<String, Object>> run(RedissonClient client, String sql)
    {
        List<String> words = tokenize(sql);
        if (words.isEmpty()) {
            throw new IllegalArgumentException("Empty Redis command");
        }
        String name = words.get(0).toUpperCase(Locale.ROOT);
        if ("SELECT".equals(name)) {
            // Connections are pooled, so switching one of them would move later queries to another database.
            throw new IllegalArgumentException("Choose the database with the database property or the URL path (jdbc:redis://host:port/2)");
        }
        Object[] args = words.subList(1, words.size()).toArray();
        CommandAsyncExecutor executor = ((Redisson) client).getCommandExecutor();
        RedisCommand<List<Object>> command = new RedisCommand<>(name, new ObjectListReplayDecoder<>());
        Object reply = executor.get(executor.writeAsync(args.length > 0 ? String.valueOf(args[0]) : null, StringCodec.INSTANCE, command, args));
        return rows(reply);
    }

    /** One row per value; lists and maps are flattened in order, as the released driver listed them. */
    static List<Map<String, Object>> rows(Object reply)
    {
        List<String> values = new ArrayList<>();
        flatten(reply, values);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String value : values) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put(COLUMN, value);
            rows.add(row);
        }
        if (rows.isEmpty() && !(reply instanceof Iterable || reply instanceof Map)) {
            rows.add(Collections.singletonMap(COLUMN, null));
        }
        return rows;
    }

    private static void flatten(Object value, List<String> values)
    {
        if (value instanceof Iterable) {
            for (Object item : (Iterable<?>) value) {
                flatten(item, values);
            }
        }
        else if (value instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                flatten(entry.getKey(), values);
                flatten(entry.getValue(), values);
            }
        }
        else if (value != null) {
            values.add(String.valueOf(value));
        }
    }
}
