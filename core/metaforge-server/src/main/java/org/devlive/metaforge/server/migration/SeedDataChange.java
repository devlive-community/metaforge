package org.devlive.metaforge.server.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import lombok.Getter;
import lombok.Setter;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Inserts the baseline catalogue from a JSON resource with bound parameters, so the
 * same seed works on every supported database (no vendor literals, no fixed ids).
 * Menus reference their parent and roles their menus by code.
 */
@Getter
@Setter
public class SeedDataChange
        implements CustomTaskChange
{
    private String resource;

    @Override
    public void execute(Database database)
            throws CustomChangeException
    {
        Connection connection = ((JdbcConnection) database.getConnection()).getUnderlyingConnection();
        try (InputStream stream = SeedDataChange.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) {
                throw new CustomChangeException("Seed resource not found: " + resource);
            }
            JsonNode seed = new ObjectMapper().readTree(stream);
            Timestamp now = new Timestamp(System.currentTimeMillis());
            seedRoles(connection, seed.path("roles"), now);
            seedMenus(connection, seed.path("menus"), now);
            seedRoleMenus(connection, seed.path("roleMenus"));
            seedFunctions(connection, seed.path("functions"), now);
            seedSchedules(connection, seed.path("schedules"), now);
        }
        catch (CustomChangeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new CustomChangeException("Failed to apply seed " + resource, e);
        }
    }

    private void seedRoles(Connection connection, JsonNode roles, Timestamp now)
            throws SQLException
    {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO metaforge_role (code, name, description, active, create_time, update_time) VALUES (?, ?, ?, ?, ?, ?)")) {
            for (JsonNode role : roles) {
                insert.setString(1, role.path("code").asText());
                insert.setString(2, role.path("name").asText());
                setString(insert, 3, role.path("description"));
                insert.setBoolean(4, role.path("active").asBoolean(true));
                insert.setTimestamp(5, now);
                insert.setTimestamp(6, now);
                insert.executeUpdate();
            }
        }
    }

    private void seedMenus(Connection connection, JsonNode menus, Timestamp now)
            throws SQLException
    {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO metaforge_menu (code, name, description, url, group_name, sorted, type, parent, active, i18n_key, icon, redirect, is_new, create_time, update_time) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?, 0, ?, ?, ?)")) {
            for (JsonNode menu : menus) {
                insert.setString(1, menu.path("code").asText());
                insert.setString(2, menu.path("name").asText());
                setString(insert, 3, menu.path("description"));
                insert.setString(4, menu.path("url").asText());
                setString(insert, 5, menu.path("group"));
                insert.setInt(6, menu.path("sorted").asInt());
                insert.setString(7, menu.path("type").asText("VIEW"));
                insert.setBoolean(8, menu.path("active").asBoolean(true));
                setString(insert, 9, menu.path("i18nKey"));
                setString(insert, 10, menu.path("icon"));
                insert.setBoolean(11, menu.path("isNew").asBoolean(false));
                insert.setTimestamp(12, now);
                insert.setTimestamp(13, now);
                insert.executeUpdate();
            }
        }
        // Parents are resolved once every menu exists, independent of the seed order.
        try (PreparedStatement update = connection.prepareStatement("UPDATE metaforge_menu SET parent = ? WHERE code = ?")) {
            for (JsonNode menu : menus) {
                JsonNode parent = menu.path("parent");
                if (!parent.isTextual()) {
                    continue;
                }
                update.setLong(1, idByCode(connection, "metaforge_menu", parent.asText()));
                update.setString(2, menu.path("code").asText());
                update.executeUpdate();
            }
        }
    }

    private void seedRoleMenus(Connection connection, JsonNode roleMenus)
            throws SQLException
    {
        try (PreparedStatement insert = connection.prepareStatement("INSERT INTO metaforge_role_menu (role_id, menu_id) VALUES (?, ?)")) {
            Iterator<Map.Entry<String, JsonNode>> entries = roleMenus.fields();
            while (entries.hasNext()) {
                Map.Entry<String, JsonNode> entry = entries.next();
                long roleId = idByCode(connection, "metaforge_role", entry.getKey());
                for (JsonNode menuCode : entry.getValue()) {
                    insert.setLong(1, roleId);
                    insert.setLong(2, idByCode(connection, "metaforge_menu", menuCode.asText()));
                    insert.executeUpdate();
                }
            }
        }
    }

    private void seedFunctions(Connection connection, JsonNode functions, Timestamp now)
            throws SQLException
    {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO metaforge_function (code, name, content, description, plugin, example, type, active, create_time, update_time) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            for (JsonNode function : functions) {
                insert.setString(1, function.path("code").asText());
                insert.setString(2, function.path("name").asText());
                insert.setString(3, function.path("content").asText());
                setString(insert, 4, function.path("description"));
                insert.setString(5, function.path("plugin").asText());
                setString(insert, 6, function.path("example"));
                insert.setString(7, function.path("type").asText());
                insert.setBoolean(8, function.path("active").asBoolean(true));
                insert.setTimestamp(9, now);
                insert.setTimestamp(10, now);
                insert.executeUpdate();
            }
        }
    }

    private void seedSchedules(Connection connection, JsonNode schedules, Timestamp now)
            throws SQLException
    {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO metaforge_scheduled (code, name, description, expression, type, active, is_system, create_time, update_time) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            for (JsonNode schedule : schedules) {
                insert.setString(1, UUID.randomUUID().toString().replace("-", ""));
                insert.setString(2, schedule.path("name").asText());
                setString(insert, 3, schedule.path("description"));
                insert.setString(4, schedule.path("expression").asText());
                insert.setString(5, schedule.path("type").asText());
                insert.setBoolean(6, schedule.path("active").asBoolean(true));
                insert.setBoolean(7, schedule.path("system").asBoolean(true));
                insert.setTimestamp(8, now);
                insert.setTimestamp(9, now);
                insert.executeUpdate();
            }
        }
    }

    private static long idByCode(Connection connection, String table, String code)
            throws SQLException
    {
        try (PreparedStatement select = connection.prepareStatement("SELECT id FROM " + table + " WHERE code = ?")) {
            select.setString(1, code);
            try (ResultSet result = select.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException(String.format("Seed references unknown %s code [ %s ]", table, code));
                }
                return result.getLong(1);
            }
        }
    }

    private static void setString(PreparedStatement statement, int index, JsonNode value)
            throws SQLException
    {
        if (value.isTextual()) {
            statement.setString(index, value.asText());
        }
        else {
            statement.setNull(index, Types.VARCHAR);
        }
    }

    @Override
    public String getConfirmationMessage()
    {
        return "Baseline catalogue seeded from " + resource;
    }

    @Override
    public void setUp()
            throws SetupException
    {
    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor)
    {
    }

    @Override
    public ValidationErrors validate(Database database)
    {
        ValidationErrors errors = new ValidationErrors();
        errors.checkRequiredField("resource", resource);
        return errors;
    }
}
