package org.devlive.metaforge.service.adapter;

import org.devlive.metaforge.service.body.FilterBody;
import org.devlive.metaforge.service.body.FilterCriterionBody;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Predicate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class FilterSpecification
{
    private static final Set<String> ALLOWED_FIELDS = Set.of(
            "active", "available", "publish", "type", "protocol", "host", "syncMode", "executor", "scheduler",
            "system", "description", "isSystem", "expression", "state", "content", "mode", "ip", "device", "client",
            "context", "realtime", "group", "url", "plugin");

    private FilterSpecification()
    {
    }

    public static <T> Specification<T> of(FilterBody filter)
    {
        return (root, query, builder) -> {
            Set<String> fields = root.getModel().getAttributes().stream()
                    .map(attribute -> attribute.getName()).collect(Collectors.toSet());
            List<Predicate> predicates = new ArrayList<>();
            if (filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty()) {
                String keyword = filter.getKeyword().trim().toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                List<Predicate> matches = new ArrayList<>();
                for (String field : new String[] {"name", "code", "username", "device", "client", "ip"}) {
                    if (fields.contains(field)) {
                        matches.add(builder.like(builder.lower(root.get(field)), "%" + keyword + "%", '\\'));
                    }
                }
                predicates.add(builder.or(matches.toArray(new Predicate[0])));
            }
            if (filter.getActive() != null) {
                predicates.add(builder.equal(root.get("active"), filter.getActive()));
            }
            if (filter.getCode() != null && !filter.getCode().trim().isEmpty()) {
                predicates.add(builder.equal(root.get("code"), filter.getCode().trim()));
            }
            if (filter.getConditions() != null) {
                for (Map.Entry<String, FilterCriterionBody> entry : filter.getConditions().entrySet()) {
                    String field = entry.getKey();
                    FilterCriterionBody criterion = entry.getValue();
                    if (criterion == null || criterion.getValue() == null || criterion.getValue().trim().isEmpty()) {
                        continue;
                    }
                    if (!ALLOWED_FIELDS.contains(field) || !fields.contains(field)) {
                        throw new IllegalArgumentException("Unsupported filter: " + field);
                    }
                    String value = criterion.getValue().trim();
                    Class<?> type = root.get(field).getJavaType();
                    if (type == Boolean.class || type == boolean.class) {
                        if (!value.equals("true") && !value.equals("false")) {
                            throw new IllegalArgumentException("Invalid boolean filter: " + field);
                        }
                        predicates.add(builder.equal(root.get(field), Boolean.valueOf(value)));
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
                        predicates.add(builder.equal(root.get(field), constant));
                    }
                    else if (type == String.class) {
                        if (criterion.isExact()) {
                            predicates.add(builder.equal(root.get(field), value));
                        }
                        else {
                            String term = value.toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                            predicates.add(builder.like(builder.lower(root.get(field)), "%" + term + "%", '\\'));
                        }
                    }
                    else {
                        throw new IllegalArgumentException("Unsupported filter type: " + field);
                    }
                }
            }
            LocalDate from = date(filter.getCreatedFrom());
            LocalDate to = date(filter.getCreatedTo());
            if (from != null && to != null && from.isAfter(to)) {
                throw new IllegalArgumentException("Start date must not be after end date");
            }
            if (from != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("createTime"), boundary(from)));
            }
            if (to != null) {
                predicates.add(builder.lessThan(root.get("createTime"), boundary(to.plusDays(1))));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static LocalDate date(String value)
    {
        return value == null || value.trim().isEmpty() ? null : LocalDate.parse(value.trim());
    }

    private static Date boundary(LocalDate value)
    {
        return Date.from(value.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
