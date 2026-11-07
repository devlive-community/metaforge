package org.devlive.metaforge.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The store details a plugin ships in {@code org/devlive/metaforge/plugin/plugin.json}, next to its
 * capabilities.json: label, description, logo, type, author, supported data source versions, the
 * minimum MetaForge version and extra features. The release build turns the same file into the
 * store catalogue, so an installed plugin can still describe itself when the catalogue is
 * unavailable or does not list it (self-built plugins).
 */
@Slf4j
public final class PluginDescriptor
{
    /** The descriptor path inside every plugin jar or classes directory. */
    public static final String FILE = "org/devlive/metaforge/plugin/plugin.json";

    private static final Set<String> TEXT = Set.of("label", "description", "logo", "type", "author", "requires");
    private static final Set<String> LISTS = Set.of("supportVersion", "features");
    private static final ObjectMapper JSON = new ObjectMapper();

    // Read once per plugin class: the file ships inside the plugin and never changes while it is loaded.
    private static final ClassValue<Map<String, Object>> DECLARED = new ClassValue<>()
    {
        @Override
        protected Map<String, Object> computeValue(Class<?> type)
        {
            return load(type);
        }
    };

    private PluginDescriptor()
    {
    }

    /** The descriptor of the plugin the class belongs to; empty when the plugin has none. */
    public static Map<String, Object> of(Class<?> plugin)
    {
        return plugin == null ? Collections.emptyMap() : DECLARED.get(plugin);
    }

    private static Map<String, Object> load(Class<?> plugin)
    {
        URL descriptor = descriptor(plugin);
        if (descriptor == null) {
            return Collections.emptyMap();
        }
        try {
            URLConnection connection = descriptor.openConnection();
            // Plugins are reloaded from their jars; a cached jar would stay open and stale.
            connection.setUseCaches(false);
            try (InputStream stream = connection.getInputStream()) {
                return read(stream);
            }
        }
        catch (FileNotFoundException error) {
            return Collections.emptyMap();
        }
        catch (IOException error) {
            // A broken descriptor only costs the offline store details, never the plugin itself.
            log.warn("Ignoring unreadable plugin descriptor {}: {}", descriptor, error.getMessage());
            return Collections.emptyMap();
        }
    }

    /** Keeps the known fields with the expected types; anything else is ignored. */
    static Map<String, Object> read(InputStream stream)
            throws IOException
    {
        JsonNode root = JSON.readTree(stream);
        if (root == null || !root.isObject()) {
            return Collections.emptyMap();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        root.fields().forEachRemaining(field -> {
            JsonNode value = field.getValue();
            if (TEXT.contains(field.getKey()) && value.isTextual() && !value.asText().isBlank()) {
                out.put(field.getKey(), value.asText());
            }
            else if (LISTS.contains(field.getKey()) && value.isArray()) {
                List<String> items = new ArrayList<>();
                value.forEach(item -> {
                    if (item.isTextual() && !item.asText().isBlank()) {
                        items.add(item.asText());
                    }
                });
                out.put(field.getKey(), Collections.unmodifiableList(items));
            }
            else if ("i18nFormat".equals(field.getKey()) && value.isBoolean()) {
                out.put(field.getKey(), value.asBoolean());
            }
        });
        return Collections.unmodifiableMap(out);
    }

    /**
     * The plugin.json in the jar or classes directory the class comes from. Every plugin uses the
     * same path, so a lookup by name could find another plugin's file on a shared class path.
     */
    private static URL descriptor(Class<?> type)
    {
        CodeSource source = type.getProtectionDomain() == null ? null : type.getProtectionDomain().getCodeSource();
        if (source == null || source.getLocation() == null) {
            return null;
        }
        try {
            String location = source.getLocation().toString();
            return location.endsWith(".jar") ? new URL("jar:" + location + "!/" + FILE) : new URL(source.getLocation(), FILE);
        }
        catch (MalformedURLException error) {
            return null;
        }
    }
}
