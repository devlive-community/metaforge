package org.devlive.metaforge.service.persistence;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.service.body.FilterBody;
import org.devlive.metaforge.service.body.FilterCriterionBody;
import org.devlive.metaforge.service.body.OrderBody;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jdbc.core.convert.EntityRowMapper;
import org.springframework.data.jdbc.core.convert.JdbcConverter;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.dialect.LimitClause;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Builds the filtered, sorted and paged listing queries behind the list pages as portable SQL.
 * Only mapped properties can be filtered or sorted, and values are always bound parameters.
 */
@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Collaborators are Spring-managed singletons")
public class FilterQuery
{
    private static final Set<String> FILTERABLE = Set.of(
            "active", "available", "publish", "type", "protocol", "host", "syncMode", "executor", "scheduler",
            "system", "description", "isSystem", "expression", "state", "content", "mode", "ip", "device", "client",
            "context", "realtime", "group", "url", "plugin");
    private static final String[] KEYWORD_FIELDS = {"name", "code", "username", "device", "client", "ip"};
    private static final char ESCAPE = '!';

    private final NamedParameterJdbcOperations jdbc;
    private final JdbcConverter converter;
    private final RelationalMappingContext context;
    private final Dialect dialect;

    public FilterQuery(NamedParameterJdbcOperations jdbc, JdbcConverter converter, RelationalMappingContext context, Dialect dialect)
    {
        this.jdbc = jdbc;
        this.converter = converter;
        this.context = context;
        this.dialect = dialect;
    }

    public <T> Page<T> page(Class<T> type, FilterBody filter, Visibility visibility)
    {
        @SuppressWarnings("unchecked")
        RelationalPersistentEntity<T> entity = (RelationalPersistentEntity<T>) context.getRequiredPersistentEntity(type);
        Map<String, RelationalPersistentProperty> properties = new HashMap<>();
        entity.doWithProperties((RelationalPersistentProperty property) -> {
            if (!property.isTransient() && !property.isEntity()) {
                properties.put(property.getName(), property);
                // list-valued columns are stored as text in a "<name>Value" field
                if (property.getName().endsWith("Value")) {
                    properties.putIfAbsent(property.getName().substring(0, property.getName().length() - "Value".length()), property);
                }
            }
        });
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        List<String> where = new ArrayList<>();
        keyword(filter, properties, where, parameters);
        if (filter.getActive() != null) {
            where.add(column(properties, "active") + " = :active");
            parameters.addValue("active", filter.getActive());
        }
        if (filter.getCode() != null && !filter.getCode().trim().isEmpty()) {
            where.add(column(properties, "code") + " = :code");
            parameters.addValue("code", filter.getCode().trim());
        }
        conditions(filter, properties, where, parameters);
        createdRange(filter, properties, where, parameters);
        visibility(visibility, properties, where, parameters);

        String table = entity.getTableName().getReference();
        String condition = where.isEmpty() ? "" : " WHERE " + String.join(" AND ", where);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + condition, parameters, Long.class);
        int size = filter.getSize() == null ? 10 : Math.max(1, filter.getSize());
        int page = filter.getPage() == null ? 1 : Math.max(1, filter.getPage());
        String sql = "SELECT * FROM " + table + condition + " ORDER BY " + orders(filter, properties, entity);
        LimitClause limit = dialect.limit();
        sql = sql + " " + limit.getLimitOffset(size, (long) (page - 1) * size);
        List<T> content = jdbc.query(sql, parameters, new EntityRowMapper<>(entity, converter));
        return new PageImpl<>(content, PageRequest.of(page - 1, size), total == null ? 0 : total);
    }

    private void keyword(FilterBody filter, Map<String, RelationalPersistentProperty> properties, List<String> where, MapSqlParameterSource parameters)
    {
        if (filter.getKeyword() == null || filter.getKeyword().trim().isEmpty()) {
            return;
        }
        List<String> matches = new ArrayList<>();
        for (String field : KEYWORD_FIELDS) {
            if (properties.containsKey(field)) {
                matches.add(like(column(properties, field)));
            }
        }
        if (matches.isEmpty()) {
            where.add("1 = 0");
            return;
        }
        where.add("(" + String.join(" OR ", matches).replace(":like", ":keyword") + ")");
        parameters.addValue("keyword", pattern(filter.getKeyword()));
    }

    private void conditions(FilterBody filter, Map<String, RelationalPersistentProperty> properties, List<String> where, MapSqlParameterSource parameters)
    {
        if (filter.getConditions() == null) {
            return;
        }
        int index = 0;
        for (Map.Entry<String, FilterCriterionBody> entry : filter.getConditions().entrySet()) {
            String field = entry.getKey();
            FilterCriterionBody criterion = entry.getValue();
            if (criterion == null || criterion.getValue() == null || criterion.getValue().trim().isEmpty()) {
                continue;
            }
            if (!FILTERABLE.contains(field) || !properties.containsKey(field)) {
                throw new IllegalArgumentException("Unsupported filter: " + field);
            }
            String value = criterion.getValue().trim();
            String parameter = "condition" + index++;
            Class<?> type = properties.get(field).getType();
            String column = column(properties, field);
            if (type == Boolean.class || type == boolean.class) {
                if (!value.equals("true") && !value.equals("false")) {
                    throw new IllegalArgumentException("Invalid boolean filter: " + field);
                }
                where.add(column + " = :" + parameter);
                parameters.addValue(parameter, Boolean.valueOf(value));
            }
            else if (type.isEnum()) {
                Object constant = null;
                for (Object candidate : type.getEnumConstants()) {
                    if (((Enum<?>) candidate).name().equals(value)) {
                        constant = candidate;
                    }
                }
                if (constant == null) {
                    throw new IllegalArgumentException("Invalid enum filter: " + field);
                }
                where.add(column + " = :" + parameter);
                parameters.addValue(parameter, ((Enum<?>) constant).name());
            }
            else if (type == String.class) {
                if (criterion.isExact()) {
                    where.add(column + " = :" + parameter);
                    parameters.addValue(parameter, value);
                }
                else {
                    where.add(like(column).replace(":like", ":" + parameter));
                    parameters.addValue(parameter, pattern(value));
                }
            }
            else {
                throw new IllegalArgumentException("Unsupported filter type: " + field);
            }
        }
    }

    private void createdRange(FilterBody filter, Map<String, RelationalPersistentProperty> properties, List<String> where, MapSqlParameterSource parameters)
    {
        LocalDate from = date(filter.getCreatedFrom());
        LocalDate to = date(filter.getCreatedTo());
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Start date must not be after end date");
        }
        if (from != null) {
            where.add(column(properties, "createTime") + " >= :createdFrom");
            parameters.addValue("createdFrom", boundary(from));
        }
        if (to != null) {
            where.add(column(properties, "createTime") + " < :createdTo");
            parameters.addValue("createdTo", boundary(to.plusDays(1)));
        }
    }

    private void visibility(Visibility visibility, Map<String, RelationalPersistentProperty> properties, List<String> where, MapSqlParameterSource parameters)
    {
        if (visibility == null || visibility.anyOf().isEmpty()) {
            return;
        }
        List<String> any = new ArrayList<>();
        int index = 0;
        for (Map.Entry<String, Object> entry : visibility.anyOf().entrySet()) {
            String parameter = "visible" + index++;
            if (entry.getValue() == null) {
                any.add("1 = 0");
                continue;
            }
            any.add(column(properties, entry.getKey()) + " = :" + parameter);
            parameters.addValue(parameter, entry.getValue());
        }
        where.add("(" + String.join(" OR ", any) + ")");
    }

    private String orders(FilterBody filter, Map<String, RelationalPersistentProperty> properties, RelationalPersistentEntity<?> entity)
    {
        Set<String> seen = new LinkedHashSet<>();
        List<String> orders = new ArrayList<>();
        if (filter.getOrders() != null) {
            for (OrderBody order : filter.getOrders()) {
                String direction = order.getOrder() == null ? "" : order.getOrder().toLowerCase(Locale.ROOT);
                if ((!direction.equals("asc") && !direction.equals("desc")) || !seen.add(order.getColumn())) {
                    continue;
                }
                if (!properties.containsKey(order.getColumn())) {
                    throw new IllegalArgumentException("Unsupported order: " + order.getColumn());
                }
                orders.add(column(properties, order.getColumn()) + " " + direction.toUpperCase(Locale.ROOT));
            }
        }
        // a unique tie-breaker keeps pages stable and satisfies OFFSET/FETCH dialects
        orders.add(entity.getRequiredIdProperty().getColumnName().getReference() + " ASC");
        return String.join(", ", orders);
    }

    private static String column(Map<String, RelationalPersistentProperty> properties, String name)
    {
        RelationalPersistentProperty property = properties.get(name);
        if (property == null) {
            throw new IllegalArgumentException("Unsupported field: " + name);
        }
        return property.getColumnName().getReference();
    }

    private static String like(String column)
    {
        return "LOWER(" + column + ") LIKE :like ESCAPE '" + ESCAPE + "'";
    }

    private static String pattern(String value)
    {
        String escaped = value.trim().toLowerCase(Locale.ROOT)
                .replace(String.valueOf(ESCAPE), "" + ESCAPE + ESCAPE)
                .replace("%", ESCAPE + "%")
                .replace("_", ESCAPE + "_");
        return "%" + escaped + "%";
    }

    private static LocalDate date(String value)
    {
        return value == null || value.trim().isEmpty() ? null : LocalDate.parse(value.trim());
    }

    private static Timestamp boundary(LocalDate value)
    {
        return Timestamp.from(value.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
