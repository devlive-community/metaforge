package org.devlive.metaforge.service.repository.jdbc;

import org.devlive.metaforge.service.common.NavigationPaths;
import org.devlive.metaforge.service.record.TreeRecord;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Explicit menu projections and atomic relationship updates, independent of ORM sessions. */
@Repository
public class MenuAccessRepository
{
    public enum SaveResult
    {
        SAVED, ROLE_NOT_FOUND, INVALID_MENUS
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    public MenuAccessRepository(DataSource dataSource)
    {
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
        this.transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    public Optional<List<TreeRecord>> findRoleMenus(Long roleId)
    {
        if (roleId == null || roleId <= 0 || jdbc.queryForObject("SELECT COUNT(*) FROM metaforge_role WHERE id = :id", Map.of("id", roleId), Long.class) == 0) {
            return Optional.empty();
        }
        Set<Long> assigned = ids(jdbc.queryForList("SELECT menu_id FROM metaforge_role_menu_relation WHERE role_id = :roleId",
                Map.of("roleId", roleId.toString()), String.class));
        return Optional.of(tree(menus(assigned, true)));
    }

    public Optional<List<TreeRecord>> findUserMenus(String userCode)
    {
        if (userCode == null) {
            return Optional.empty();
        }
        List<Long> users = jdbc.queryForList("SELECT id FROM metaforge_user WHERE code = :code", Map.of("code", userCode), Long.class);
        if (users.size() != 1) {
            return Optional.empty();
        }
        Set<Long> assigned = ids(jdbc.queryForList(
                "SELECT DISTINCT rm.menu_id FROM metaforge_role_menu_relation rm "
                        + "JOIN metaforge_role r ON rm.role_id = CONCAT('', r.id) "
                        + "JOIN metaforge_user_role_relation ur ON ur.role_id = r.id WHERE ur.user_id = :userId",
                Map.of("userId", users.get(0)), String.class));
        boolean administrator = jdbc.queryForObject("SELECT COUNT(*) FROM metaforge_user_role_relation ur JOIN metaforge_role r ON r.id = ur.role_id WHERE ur.user_id = :userId AND r.code = 'ADMIN'",
                Map.of("userId", users.get(0)), Long.class) > 0;
        List<TreeRecord> all = menus(assigned, false).stream()
                .filter(menu -> administrator || !NavigationPaths.administrative(menu.getUrl())).collect(Collectors.toList());
        Map<Long, TreeRecord> byId = new LinkedHashMap<>();
        all.forEach(menu -> byId.put(menu.getId(), menu));
        Set<Long> included = new HashSet<>(assigned);
        for (Long id : assigned) {
            TreeRecord current = byId.get(id);
            Set<Long> visited = new HashSet<>();
            while (current != null && visited.add(current.getId())) {
                included.add(current.getId());
                current = byId.get(current.getParent());
            }
        }
        List<TreeRecord> visible = new ArrayList<>();
        for (TreeRecord menu : all) {
            if (included.contains(menu.getId())) {
                menu.setChecked(true);
                menu.setSelected(true);
                visible.add(menu);
            }
        }
        return Optional.of(tree(visible));
    }

    public SaveResult replaceRoleMenus(Long roleId, Set<Long> menuIds)
    {
        if (roleId == null || roleId <= 0) {
            return SaveResult.ROLE_NOT_FOUND;
        }
        if (menuIds == null || menuIds.stream().anyMatch(id -> id == null || id <= 0)) {
            return SaveResult.INVALID_MENUS;
        }
        return transaction.execute(status -> {
            MapSqlParameterSource parameters = new MapSqlParameterSource("id", roleId).addValue("roleId", roleId.toString());
            if (jdbc.queryForList("SELECT id FROM metaforge_role WHERE id = :id FOR UPDATE", parameters, Long.class).isEmpty()) {
                return SaveResult.ROLE_NOT_FOUND;
            }
            if (!menuIds.isEmpty()) {
                parameters.addValue("menuIds", menuIds);
                List<Long> existing = jdbc.queryForList("SELECT id FROM metaforge_menu WHERE type = 'VIEW' AND id IN (:menuIds) ORDER BY id FOR UPDATE", parameters, Long.class);
                if (existing.size() != menuIds.size()) {
                    return SaveResult.INVALID_MENUS;
                }
            }
            jdbc.update("DELETE FROM metaforge_role_menu_relation WHERE role_id = :roleId", parameters);
            if (!menuIds.isEmpty()) {
                MapSqlParameterSource[] batch = menuIds.stream()
                        .map(id -> new MapSqlParameterSource("roleId", roleId.toString()).addValue("menuId", id.toString()))
                        .toArray(MapSqlParameterSource[]::new);
                jdbc.batchUpdate("INSERT INTO metaforge_role_menu_relation (role_id, menu_id) VALUES (:roleId, :menuId)", batch);
            }
            return SaveResult.SAVED;
        });
    }

    private List<TreeRecord> menus(Set<Long> assigned, boolean viewsOnly)
    {
        String sql = "SELECT id, name, url, sorted, code, i18n_key, icon, is_new, description, parent FROM metaforge_menu"
                + (viewsOnly ? " WHERE type = 'VIEW'" : "") + " ORDER BY sorted, id";
        return jdbc.query(sql, Map.of(), (row, index) -> NavigationPaths.normalize(TreeRecord.builder()
                .id(row.getLong("id")).title(row.getString("name")).url(row.getString("url"))
                .sorted(row.getInt("sorted")).code(row.getString("code")).i18nKey(row.getString("i18n_key"))
                .icon(row.getString("icon")).isNew(row.getBoolean("is_new")).description(row.getString("description"))
                .parent(row.getLong("parent")).checked(assigned.contains(row.getLong("id"))).selected(assigned.contains(row.getLong("id")))
                .build()));
    }

    private Set<Long> ids(List<String> values)
    {
        Set<Long> ids = new HashSet<>();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            try {
                ids.add(Long.parseLong(value.trim()));
            }
            catch (NumberFormatException ignored) {
                // Legacy text relationship columns may contain dangling non-numeric values.
            }
        }
        return ids;
    }

    private List<TreeRecord> tree(List<TreeRecord> menus)
    {
        Map<Long, TreeRecord> byId = new LinkedHashMap<>();
        menus.forEach(menu -> byId.put(menu.getId(), menu));
        List<TreeRecord> roots = new ArrayList<>();
        for (TreeRecord menu : menus) {
            TreeRecord parent = byId.get(menu.getParent());
            Set<Long> visited = new HashSet<>();
            TreeRecord ancestor = parent;
            boolean cyclic = false;
            while (ancestor != null) {
                if (ancestor.getId().equals(menu.getId()) || !visited.add(ancestor.getId())) {
                    cyclic = true;
                    break;
                }
                ancestor = byId.get(ancestor.getParent());
            }
            if (parent == null || cyclic) {
                roots.add(menu);
            }
            else {
                if (parent.getChildren() == null) {
                    parent.setChildren(new ArrayList<>());
                }
                parent.getChildren().add(menu);
            }
        }
        return roots;
    }
}
