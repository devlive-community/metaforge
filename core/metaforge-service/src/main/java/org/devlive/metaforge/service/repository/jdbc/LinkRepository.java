package org.devlive.metaforge.service.repository.jdbc;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * The many-to-many link tables (user-role, dashboard-report), read and replaced with explicit SQL.
 * Callers run replacements inside their own transaction.
 */
@Repository
public class LinkRepository
{
    private final NamedParameterJdbcOperations jdbc;

    public LinkRepository(NamedParameterJdbcOperations jdbc)
    {
        this.jdbc = jdbc;
    }

    public List<Long> roleIdsOfUser(Long userId)
    {
        return jdbc.queryForList("SELECT role_id FROM metaforge_user_role WHERE user_id = :id", Map.of("id", userId), Long.class);
    }

    public void replaceUserRoles(Long userId, Collection<Long> roleIds)
    {
        replace("metaforge_user_role", "user_id", "role_id", userId, roleIds);
    }

    public void deleteUser(Long userId)
    {
        jdbc.update("DELETE FROM metaforge_user_role WHERE user_id = :id", Map.of("id", userId));
    }

    public void deleteRole(Long roleId)
    {
        jdbc.update("DELETE FROM metaforge_user_role WHERE role_id = :id", Map.of("id", roleId));
        jdbc.update("DELETE FROM metaforge_role_menu WHERE role_id = :id", Map.of("id", roleId));
    }

    public void deleteMenu(Long menuId)
    {
        jdbc.update("DELETE FROM metaforge_role_menu WHERE menu_id = :id", Map.of("id", menuId));
    }

    public List<Long> reportIdsOfDashboard(Long dashboardId)
    {
        return jdbc.queryForList("SELECT report_id FROM metaforge_dashboard_report WHERE dashboard_id = :id", Map.of("id", dashboardId), Long.class);
    }

    public void replaceDashboardReports(Long dashboardId, Collection<Long> reportIds)
    {
        replace("metaforge_dashboard_report", "dashboard_id", "report_id", dashboardId, reportIds);
    }

    public void deleteDashboard(Long dashboardId)
    {
        jdbc.update("DELETE FROM metaforge_dashboard_report WHERE dashboard_id = :id", Map.of("id", dashboardId));
    }

    public void deleteReport(Long reportId)
    {
        jdbc.update("DELETE FROM metaforge_dashboard_report WHERE report_id = :id", Map.of("id", reportId));
    }

    private void replace(String table, String ownerColumn, String targetColumn, Long ownerId, Collection<Long> targetIds)
    {
        jdbc.update("DELETE FROM " + table + " WHERE " + ownerColumn + " = :id", Map.of("id", ownerId));
        List<MapSqlParameterSource> rows = new ArrayList<>();
        for (Long targetId : new LinkedHashSet<>(targetIds)) {
            if (targetId != null) {
                rows.add(new MapSqlParameterSource("owner", ownerId).addValue("target", targetId));
            }
        }
        if (!rows.isEmpty()) {
            jdbc.batchUpdate("INSERT INTO " + table + " (" + ownerColumn + ", " + targetColumn + ") VALUES (:owner, :target)",
                    rows.toArray(new MapSqlParameterSource[0]));
        }
    }
}
