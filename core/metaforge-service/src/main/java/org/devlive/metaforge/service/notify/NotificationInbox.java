package org.devlive.metaforge.service.notify;

import com.fasterxml.jackson.annotation.JsonFormat;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.devlive.metaforge.service.enums.NotificationScene;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * One user's site notifications: filtered pages, counts, read state and removal. Every statement is
 * limited to the given user, so nobody reads or changes another user's notifications by code.
 */
@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Collaborators are Spring-managed singletons")
public class NotificationInbox
{
    static final int MAX_PAGE_SIZE = 100;
    private static final String TABLE = "metaforge_notification";
    private static final char ESCAPE = '!';
    private static final String COLUMNS = "code, name, content, type, is_read, create_time, entity_type, entity_code, entity_name, entity_exists";

    private final NamedParameterJdbcOperations jdbc;
    private final Dialect dialect;

    public NotificationInbox(NamedParameterJdbcOperations jdbc, Dialect dialect)
    {
        this.jdbc = jdbc;
        this.dialect = dialect;
    }

    /** What to list: read state (null for all), a category (null for all), words in the title or content. */
    @Data
    @NoArgsConstructor
    public static class Query
    {
        private Boolean read;
        private String category;
        private String keyword;
        private Integer page = 1;
        private Integer size = 20;
    }

    @Data
    @NoArgsConstructor
    @SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "Plain response object")
    public static class Item
    {
        private String code;
        private String title;
        private String content;
        private String scene;
        private String category;
        private boolean read;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Date createTime;
        private String entityType;
        private String entityCode;
        private String entityName;
        private boolean entityExists;
    }

    @Data
    @NoArgsConstructor
    @SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "Plain response object")
    public static class Page
    {
        private List<Item> items = new ArrayList<>();
        private long total;
        private int page;
        private int size;
    }

    @Data
    @NoArgsConstructor
    @SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "Plain response object")
    public static class Summary
    {
        private long total;
        private long unread;
        /** Totals by category, system first, then the scene categories in their order. */
        private Map<String, Long> categories = new LinkedHashMap<>();
    }

    public Page page(Long userId, Query query)
    {
        MapSqlParameterSource parameters = new MapSqlParameterSource("user", userId);
        String where = where(query, parameters);
        int size = Math.min(MAX_PAGE_SIZE, Math.max(1, query.getSize() == null ? 20 : query.getSize()));
        int page = Math.max(1, query.getPage() == null ? 1 : query.getPage());
        Page result = new Page();
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM " + TABLE + where, parameters, Long.class);
        result.setTotal(total == null ? 0 : total);
        result.setPage(page);
        result.setSize(size);
        String sql = "SELECT " + COLUMNS + " FROM " + TABLE + where + " ORDER BY create_time DESC, id DESC "
                + dialect.limit().getLimitOffset(size, (long) (page - 1) * size);
        result.setItems(jdbc.query(sql, parameters, (row, index) -> item(row)));
        return result;
    }

    public Optional<Item> find(Long userId, String code)
    {
        MapSqlParameterSource parameters = new MapSqlParameterSource("user", userId).addValue("code", code);
        return jdbc.query("SELECT " + COLUMNS + " FROM " + TABLE + " WHERE user_id = :user AND code = :code", parameters, (row, index) -> item(row))
                .stream().findFirst();
    }

    public Summary summary(Long userId)
    {
        Summary summary = new Summary();
        summary.getCategories().put(NotificationScene.SYSTEM, 0L);
        for (NotificationScene.Category category : NotificationScene.Category.values()) {
            summary.getCategories().put(category.name(), 0L);
        }
        jdbc.query("SELECT type, is_read, COUNT(*) AS total FROM " + TABLE + " WHERE user_id = :user GROUP BY type, is_read",
                new MapSqlParameterSource("user", userId), row -> {
                    long count = row.getLong("total");
                    summary.setTotal(summary.getTotal() + count);
                    if (!row.getBoolean("is_read")) {
                        summary.setUnread(summary.getUnread() + count);
                    }
                    summary.getCategories().merge(NotificationScene.categoryOf(row.getString("type")), count, Long::sum);
                });
        return summary;
    }

    public long unread(Long userId)
    {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM " + TABLE + " WHERE user_id = :user AND is_read = :unread",
                new MapSqlParameterSource("user", userId).addValue("unread", false), Long.class);
        return count == null ? 0 : count;
    }

    /** Marks one notification read or unread; false when the user has no such notification. */
    public boolean mark(Long userId, String code, boolean read)
    {
        return jdbc.update("UPDATE " + TABLE + " SET is_read = :read WHERE user_id = :user AND code = :code",
                new MapSqlParameterSource("user", userId).addValue("code", code).addValue("read", read)) > 0;
    }

    /** Marks every unread notification read, or only those of one category; returns how many changed. */
    public int markAllRead(Long userId, String category)
    {
        Query query = new Query();
        query.setRead(false);
        query.setCategory(category);
        MapSqlParameterSource parameters = new MapSqlParameterSource("user", userId).addValue("done", true);
        return jdbc.update("UPDATE " + TABLE + " SET is_read = :done" + where(query, parameters), parameters);
    }

    public boolean delete(Long userId, String code)
    {
        return jdbc.update("DELETE FROM " + TABLE + " WHERE user_id = :user AND code = :code",
                new MapSqlParameterSource("user", userId).addValue("code", code)) > 0;
    }

    /** Removes every read notification of the user; returns how many went. */
    public int deleteRead(Long userId)
    {
        return jdbc.update("DELETE FROM " + TABLE + " WHERE user_id = :user AND is_read = :read",
                new MapSqlParameterSource("user", userId).addValue("read", true));
    }

    private static String where(Query query, MapSqlParameterSource parameters)
    {
        List<String> where = new ArrayList<>();
        where.add("user_id = :user");
        if (query.getRead() != null) {
            where.add("is_read = :isRead");
            parameters.addValue("isRead", query.getRead());
        }
        String category = query.getCategory();
        if (category != null && !category.isBlank()) {
            if (NotificationScene.SYSTEM.equals(category)) {
                where.add("(type IS NULL OR type NOT IN (:known))");
                parameters.addValue("known", NotificationScene.knownTypes());
            }
            else {
                NotificationScene.Category scenes = NotificationScene.Category.valueOf(category);
                where.add("type IN (:types)");
                parameters.addValue("types", NotificationScene.typesOf(scenes));
            }
        }
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            String escaped = query.getKeyword().trim().toLowerCase(Locale.ROOT)
                    .replace(String.valueOf(ESCAPE), "" + ESCAPE + ESCAPE)
                    .replace("%", ESCAPE + "%")
                    .replace("_", ESCAPE + "_");
            where.add("(LOWER(name) LIKE :keyword ESCAPE '" + ESCAPE + "' OR LOWER(content) LIKE :keyword ESCAPE '" + ESCAPE + "')");
            parameters.addValue("keyword", "%" + escaped + "%");
        }
        return " WHERE " + String.join(" AND ", where);
    }

    private static Item item(ResultSet row)
            throws SQLException
    {
        Item item = new Item();
        item.setCode(row.getString("code"));
        item.setTitle(row.getString("name"));
        item.setContent(row.getString("content"));
        item.setScene(row.getString("type"));
        item.setCategory(NotificationScene.categoryOf(row.getString("type")));
        item.setRead(row.getBoolean("is_read"));
        item.setCreateTime(row.getTimestamp("create_time"));
        item.setEntityType(row.getString("entity_type"));
        item.setEntityCode(row.getString("entity_code"));
        item.setEntityName(row.getString("entity_name"));
        item.setEntityExists(row.getBoolean("entity_exists"));
        return item;
    }
}
