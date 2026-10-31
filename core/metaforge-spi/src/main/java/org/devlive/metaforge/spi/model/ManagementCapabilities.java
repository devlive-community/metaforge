package org.devlive.metaforge.spi.model;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.devlive.metaforge.common.view.EntityView;

import java.io.IOException;
import java.io.InputStream;
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
        // A service extending another plugin's service inherits its declaration unless it has its own.
        Class<?> owner = service;
        while (owner != null && owner != Object.class && owner.getResource(owner.getSimpleName() + ".capabilities.json") == null) {
            owner = owner.getSuperclass();
        }
        if (owner == null || owner == Object.class) {
            return none();
        }
        String resource = owner.getSimpleName() + ".capabilities.json";
        try (InputStream stream = owner.getResourceAsStream(resource)) {
            return stream == null ? none() : read(resource, stream);
        }
        catch (IOException error) {
            throw new IllegalStateException("Reading " + resource + " failed", error);
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
