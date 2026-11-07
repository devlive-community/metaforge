package org.devlive.metaforge.server.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.plugin.PluginMetadata;
import org.devlive.metaforge.plugin.PluginType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "JdbcTemplate is a shared Spring bean")
public class PluginReferences
{
    private final JdbcTemplate jdbc;

    public PluginReferences(JdbcTemplate jdbc)
    {
        this.jdbc = jdbc;
    }

    /** Missing entries mean the plugin type has no tracked business references. */
    public Map<String, List<Reference>> list(List<PluginMetadata> plugins)
    {
        Map<String, List<Reference>> references = new LinkedHashMap<>();
        Map<PluginType, Map<String, List<Reference>>> byType = new LinkedHashMap<>();
        for (PluginMetadata plugin : plugins) {
            PluginType type = plugin.getType();
            if (type != PluginType.CONNECTOR && type != PluginType.EXECUTOR && type != PluginType.SCHEDULER) {
                continue;
            }
            List<Reference> items = new ArrayList<>();
            references.put(plugin.getName(), items);
            Map<String, List<Reference>> names = byType.computeIfAbsent(type, key -> new LinkedHashMap<>());
            names.put(normalize(plugin.getName()), items);
        }
        Map<String, List<Reference>> executors = byType.get(PluginType.EXECUTOR);
        if (executors != null && executors.containsKey("metaforge")) {
            executors.putIfAbsent("local", executors.get("metaforge"));
            executors.putIfAbsent("localexecutor", executors.get("metaforge"));
        }
        collect(byType.get(PluginType.CONNECTOR), "source",
                "SELECT source_type, name FROM metaforge_source ORDER BY name");
        collect(executors, "dataset", "SELECT executor, name FROM metaforge_dataset ORDER BY name");
        collect(executors, "workflow", "SELECT executor, name FROM metaforge_workflow ORDER BY name");
        collect(byType.get(PluginType.SCHEDULER), "dataset", "SELECT scheduler, name FROM metaforge_dataset ORDER BY name");
        return references;
    }

    public boolean isInUse(String name, List<PluginMetadata> plugins)
    {
        PluginMetadata plugin = plugins.stream().filter(item -> item.getName().equals(name)).findFirst().orElse(null);
        if (plugin == null) {
            return false;
        }
        String normalized = normalize(name);
        if (plugin.getType() == PluginType.CONNECTOR) {
            return exists("SELECT COUNT(*) FROM metaforge_source WHERE LOWER(source_type) = ?", normalized);
        }
        if (plugin.getType() == PluginType.EXECUTOR) {
            if (exists("SELECT COUNT(*) FROM metaforge_dataset WHERE LOWER(executor) = ?", normalized)
                    || exists("SELECT COUNT(*) FROM metaforge_workflow WHERE LOWER(executor) = ?", normalized)) {
                return true;
            }
            if ("metaforge".equals(normalized)) {
                for (String alias : List.of("local", "localexecutor")) {
                    boolean registered = plugins.stream().anyMatch(item -> normalize(item.getName()).equals(alias));
                    if (!registered && (exists("SELECT COUNT(*) FROM metaforge_dataset WHERE LOWER(executor) = ?", alias)
                            || exists("SELECT COUNT(*) FROM metaforge_workflow WHERE LOWER(executor) = ?", alias))) {
                        return true;
                    }
                }
            }
        }
        if (plugin.getType() == PluginType.SCHEDULER) {
            return exists("SELECT COUNT(*) FROM metaforge_dataset WHERE LOWER(scheduler) = ?", normalized);
        }
        return false;
    }

    private boolean exists(String sql, String value)
    {
        Long count = jdbc.queryForObject(sql, Long.class, value);
        return count != null && count > 0;
    }

    private void collect(Map<String, List<Reference>> names, String kind, String sql)
    {
        if (names == null) {
            return;
        }
        jdbc.query(sql, result -> {
            List<Reference> items = names.get(normalize(result.getString(1)));
            if (items != null) {
                items.add(new Reference(kind, result.getString(2)));
            }
        });
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    public static final class Reference
    {
        private final String kind;
        private final String name;

        Reference(String kind, String name)
        {
            this.kind = kind;
            this.name = name;
        }

        public String getKind()
        {
            return kind;
        }

        public String getName()
        {
            return name;
        }
    }
}
