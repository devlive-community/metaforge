package org.devlive.metaforge.spi.model;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.devlive.metaforge.common.view.EntityView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.CodeSource;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Plugin-owned management contract: the kind of source and the capabilities its plugin declares
 * (see {@link ManagementCapability}). Capability names describe implemented operations, not user
 * permissions.
 */
public final class ManagementCapabilities
{
    @JsonView({EntityView.UserView.class, EntityView.AdminView.class})
    private final String kind;
    @JsonView({EntityView.UserView.class, EntityView.AdminView.class})
    private final Set<String> capabilities;

    public ManagementCapabilities(String kind, Set<String> capabilities)
    {
        this.kind = kind;
        this.capabilities = Set.copyOf(capabilities);
    }

    public String getKind()
    {
        return kind;
    }

    public Set<String> getCapabilities()
    {
        return capabilities;
    }

    public boolean supports(String capability)
    {
        return capabilities.contains(capability);
    }

    /** The declaration file in each plugin's jar, in the plugin package directory. */
    public static final String FILE = "org/devlive/metaforge/plugin/capabilities.json";

    /** A source without management: no workspace, no management entries. */
    public static ManagementCapabilities none()
    {
        return new ManagementCapabilities("unknown", Set.of());
    }

    /**
     * The capabilities a plugin declares in {@code <ServiceClass>.capabilities.json} next to its
     * service class: {@code {"kind": "relational", "capabilities": ["database:list", ...]}}. The
     * file is named after the class so plugins sharing a class path never read each other's. No
     * file means no management; an unknown kind or capability is an error in the plugin.
     */
    public static ManagementCapabilities declaredBy(Class<?> service)
    {
        return DECLARED.get(service);
    }

    // Read once per service class: the declaration ships inside the plugin and never changes while it is loaded.
    private static final ClassValue<ManagementCapabilities> DECLARED = new ClassValue<>()
    {
        @Override
        protected ManagementCapabilities computeValue(Class<?> type)
        {
            return load(type);
        }
    };

    private static ManagementCapabilities load(Class<?> service)
    {
        // A service extending another plugin's service inherits its declaration unless its own jar has one.
        for (Class<?> owner = service; owner != null && owner != Object.class; owner = owner.getSuperclass()) {
            URL declaration = declaration(owner);
            if (declaration == null) {
                continue;
            }
            try {
                URLConnection connection = declaration.openConnection();
                // Plugins are reloaded from their jars; a cached jar would stay open and stale.
                connection.setUseCaches(false);
                try (InputStream stream = connection.getInputStream()) {
                    return read(declaration.toString(), stream);
                }
            }
            catch (FileNotFoundException error) {
                continue;
            }
            catch (IOException error) {
                throw new IllegalStateException("Reading " + declaration + " failed", error);
            }
        }
        return none();
    }

    /**
     * The capabilities.json of the plugin package in the jar or classes directory the class comes
     * from. Every plugin puts it at the same path, so it is read from the class's own code source:
     * a lookup by name would find whichever plugin comes first when several share a class path.
     */
    private static URL declaration(Class<?> type)
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

    /** Reads and checks one declaration file; {@code resource} only names it in errors. */
    public static ManagementCapabilities read(String resource, InputStream stream)
            throws IOException
    {
        JsonNode root = new ObjectMapper().readTree(stream);
        String kind = root.path("kind").asText("unknown");
        if (!ManagementCapability.KINDS.contains(kind)) {
            throw new IllegalStateException(resource + " declares the unknown kind " + kind);
        }
        Set<String> capabilities = new LinkedHashSet<>();
        for (JsonNode node : root.path("capabilities")) {
            String capability = node.asText();
            if (!ManagementCapability.of(capability).isPresent()) {
                throw new IllegalStateException(resource + " declares the unknown capability " + capability);
            }
            capabilities.add(capability);
        }
        return new ManagementCapabilities(kind, capabilities);
    }
}
