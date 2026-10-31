package org.devlive.metaforge.spi.model;

import com.fasterxml.jackson.annotation.JsonView;
import org.devlive.metaforge.common.view.EntityView;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** Plugin-owned management contract. Capability names describe implemented operations, not user permissions. */
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

    public static ManagementCapabilities queryOnly(String kind)
    {
        return new ManagementCapabilities(kind, Set.of("query:execute", "tab:sql"));
    }

    public static ManagementCapabilities forJdbc(String connectType)
    {
        String type = connectType == null ? "" : connectType.toLowerCase(Locale.ROOT);
        if (type.equals("redis")) {
            return queryOnly("key-value");
        }
        if (type.equals("mongodb")) {
            return queryOnly("document");
        }
        if (type.equals("neo4j")) {
            return queryOnly("graph");
        }
        if (type.isEmpty() || type.equals("metaforge")) {
            return queryOnly("unknown");
        }
        Set<String> capabilities = new LinkedHashSet<>(Set.of(
                "query:execute", "database:list", "table:list", "column:read", "index:read", "foreign-key:read",
                "tab:structure", "tab:sql", "tab:indexes", "tab:foreignkeys"));
        // The shared DDL/data generators currently target MySQL. Other plugins opt in when they implement a dialect.
        if (type.equals("mysql")) {
            capabilities.addAll(Set.of(
                    "engine:list", "datatype:list", "database:info", "database:alter", "table:info", "table:read", "table:statement",
                    "table:create", "table:drop", "table:truncate", "table:export", "table:auto-increment",
                    "column:create", "column:modify", "column:drop", "column:reorder", "index:create", "index:drop",
                    "foreign-key:create", "foreign-key:drop", "trigger:create", "trigger:drop", "table:alter",
                    "data:insert", "data:modify", "data:delete", "trigger:read",
                    "tab:data", "tab:info", "tab:statement", "tab:triggers", "tab:er-diagram"));
        }
        return new ManagementCapabilities("relational", capabilities);
    }
}
