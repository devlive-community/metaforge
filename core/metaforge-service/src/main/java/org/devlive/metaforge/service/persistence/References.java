package org.devlive.metaforge.service.persistence;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.data.domain.Page;
import org.springframework.data.jdbc.core.JdbcAggregateOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

/**
 * Explicit replacement for JPA associations: loads the entities behind {@link Reference} fields
 * with one batched query per reference and keeps id columns in step with referenced entities.
 */
@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Collaborators are Spring-managed singletons")
public class References
{
    /** Keeps IN lists below the 1000 element limit of Oracle. */
    private static final int BATCH = 900;
    private static final Map<Class<?>, List<Link>> LINKS = new ConcurrentHashMap<>();

    private final JdbcAggregateOperations template;

    public References(JdbcAggregateOperations template)
    {
        this.template = template;
    }

    public <T> T load(T entity)
    {
        if (entity != null) {
            load(Collections.singletonList(entity));
        }
        return entity;
    }

    public <T> Page<T> load(Page<T> page)
    {
        load(page.getContent());
        return page;
    }

    public <T, C extends Collection<T>> C load(C entities)
    {
        if (entities == null || entities.isEmpty()) {
            return entities;
        }
        Class<?> type = entities.iterator().next().getClass();
        for (Link link : links(type)) {
            Set<Object> ids = new LinkedHashSet<>();
            for (T entity : entities) {
                Object id = link.id(entity);
                if (id != null) {
                    ids.add(id);
                }
            }
            Map<Object, Object> targets = findAll(link.target(), ids);
            for (T entity : entities) {
                Object id = link.id(entity);
                link.set(entity, id == null ? null : targets.get(id));
            }
        }
        return entities;
    }

    private Map<Object, Object> findAll(Class<?> target, Set<Object> ids)
    {
        Map<Object, Object> found = new HashMap<>();
        List<Object> pending = new ArrayList<>(ids);
        for (int from = 0; from < pending.size(); from += BATCH) {
            List<Object> batch = pending.subList(from, Math.min(pending.size(), from + BATCH));
            for (Object value : template.findAllById(batch, target)) {
                found.put(Ids.of(value), value);
            }
        }
        return found;
    }

    /**
     * Copies the id of every referenced entity into its id column; called before saving.
     */
    public static void syncIds(Object entity)
    {
        for (Link link : links(entity.getClass())) {
            Object target = link.get(entity);
            if (target != null) {
                ReflectionUtils.setField(link.idField, entity, Ids.of(target));
            }
        }
    }

    private static List<Link> links(Class<?> type)
    {
        return LINKS.computeIfAbsent(type, key -> {
            List<Link> links = new ArrayList<>();
            ReflectionUtils.doWithFields(key, field -> {
                Reference reference = field.getAnnotation(Reference.class);
                Field idField = requireNonNull(ReflectionUtils.findField(key, reference.value()),
                        () -> String.format("%s.%s references missing id field %s", key.getSimpleName(), field.getName(), reference.value()));
                ReflectionUtils.makeAccessible(field);
                ReflectionUtils.makeAccessible(idField);
                links.add(new Link(field, idField));
            }, field -> field.isAnnotationPresent(Reference.class));
            return links;
        });
    }

    private static final class Link
    {
        private final Field field;
        private final Field idField;

        private Link(Field field, Field idField)
        {
            this.field = field;
            this.idField = idField;
        }

        private Class<?> target()
        {
            return field.getType();
        }

        private Object id(Object entity)
        {
            return ReflectionUtils.getField(idField, entity);
        }

        private Object get(Object entity)
        {
            return ReflectionUtils.getField(field, entity);
        }

        private void set(Object entity, Object value)
        {
            ReflectionUtils.setField(field, entity, value);
        }
    }
}
