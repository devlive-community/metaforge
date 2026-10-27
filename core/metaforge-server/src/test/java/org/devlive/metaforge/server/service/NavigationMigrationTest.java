package org.devlive.metaforge.server.service;

import org.devlive.metaforge.server.runner.NavigationMigrationRunner;
import org.devlive.metaforge.service.common.NavigationPaths;
import org.devlive.metaforge.service.initializer.InstallationState;
import org.devlive.metaforge.service.record.TreeRecord;
import org.devlive.metaforge.service.repository.jdbc.MenuAccessRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

public class NavigationMigrationTest
{
    private DriverManagerDataSource dataSource;
    private JdbcTemplate jdbc;

    @BeforeEach
    public void initialize()
            throws Exception
    {
        dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema/setup-schema-h2.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema/setup-data.sql"));
        }
        jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO metaforge_user (id, username, code) VALUES (10, 'ordinary', 'ORDINARY'), (11, 'administrator', 'ADMINISTRATOR')");
        jdbc.update("INSERT INTO metaforge_user_role_relation (user_id, role_id) VALUES (10, 2), (11, 1)");
        jdbc.update("UPDATE metaforge_menu SET url = '/admin/query' WHERE code = 'QUERY'");
        jdbc.update("UPDATE metaforge_menu SET url = '/system/role' WHERE code = 'ROLE'");
        jdbc.update("UPDATE metaforge_menu SET url = '/system' WHERE code = 'SYSTEM'");
        jdbc.update("INSERT INTO metaforge_menu (id, name, code, url, type, parent) VALUES (10000, 'Custom entry', 'CUSTOM_ENTRY', '/custom', 'VIEW', 0)");
    }

    @AfterEach
    public void close()
    {
        if (jdbc != null) {
            jdbc.execute("SHUTDOWN");
        }
    }

    @Test
    public void legacyCataloguesAreRegroupedWithoutChangingIdsOrGrants()
            throws Exception
    {
        List<Map<String, Object>> grants = jdbc.queryForList("SELECT * FROM metaforge_role_menu_relation ORDER BY role_id, menu_id");
        NavigationMigrationRunner runner = new NavigationMigrationRunner(dataSource, new InstallationState(dataSource));
        runner.run();
        runner.run();
        assertEquals("/query", url("QUERY"));
        assertEquals("/admin/role", url("ROLE"));
        assertEquals("/workspace", url("MANAGEMENT"));
        assertEquals("/admin", url("SYSTEM"));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM metaforge_menu WHERE code = 'DATA_ANALYSIS'", Integer.class));
        assertEquals(4L, jdbc.queryForObject("SELECT id FROM metaforge_menu WHERE code = 'DATASOURCE'", Long.class));
        assertEquals(3L, jdbc.queryForObject("SELECT parent FROM metaforge_menu WHERE code = 'DATASET'", Long.class));
        assertEquals(8L, jdbc.queryForObject("SELECT parent FROM metaforge_menu WHERE code = 'FUNCTION'", Long.class));
        assertEquals(jdbc.queryForObject("SELECT id FROM metaforge_menu WHERE code = 'DATA_ANALYSIS'", Long.class), jdbc.queryForObject("SELECT parent FROM metaforge_menu WHERE code = 'DASHBOARD'", Long.class));
        assertEquals(grants, jdbc.queryForList("SELECT * FROM metaforge_role_menu_relation ORDER BY role_id, menu_id"));
        assertEquals("/custom", url("CUSTOM_ENTRY"));
    }

    @Test
    public void ordinaryMenusCannotExposeTheAdminAreaEvenIfAnOldGrantExists()
            throws Exception
    {
        jdbc.update("INSERT INTO metaforge_role_menu_relation (role_id, menu_id) VALUES ('2', '12')");
        new NavigationMigrationRunner(dataSource, new InstallationState(dataSource)).run();
        MenuAccessRepository menus = new MenuAccessRepository(dataSource);
        List<TreeRecord> ordinary = flatten(menus.findUserMenus("ORDINARY").orElseThrow());
        assertTrue(ordinary.stream().anyMatch(menu -> "/query".equals(menu.getUrl())));
        assertFalse(ordinary.stream().anyMatch(menu -> NavigationPaths.administrative(menu.getUrl())));
        List<TreeRecord> admin = flatten(menus.findUserMenus("ADMINISTRATOR").orElseThrow());
        assertTrue(admin.stream().anyMatch(menu -> "/admin/role".equals(menu.getUrl())));
    }

    @Test
    public void laterAdministratorCustomizationsSurviveSubsequentRestarts()
            throws Exception
    {
        NavigationMigrationRunner runner = new NavigationMigrationRunner(dataSource, new InstallationState(dataSource));
        runner.run();
        jdbc.update("UPDATE metaforge_menu SET parent = 10000, sorted = 99 WHERE code = 'QUERY'");
        jdbc.update("UPDATE metaforge_menu SET name = 'Team analytics' WHERE code = 'DATA_ANALYSIS'");
        runner.run();
        assertEquals(10000L, jdbc.queryForObject("SELECT parent FROM metaforge_menu WHERE code = 'QUERY'", Long.class));
        assertEquals(99, jdbc.queryForObject("SELECT sorted FROM metaforge_menu WHERE code = 'QUERY'", Integer.class));
        assertEquals("Team analytics", jdbc.queryForObject("SELECT name FROM metaforge_menu WHERE code = 'DATA_ANALYSIS'", String.class));
    }

    @Test
    public void migrationFailureRollsBackEarlierUrlChanges()
    {
        jdbc.execute("ALTER TABLE metaforge_menu ADD CONSTRAINT reject_query CHECK (url <> '/query')");
        assertThrows(Exception.class, () -> new NavigationMigrationRunner(dataSource, new InstallationState(dataSource)).run());
        assertEquals("/system", url("SYSTEM"));
        assertEquals("/admin/query", url("QUERY"));
    }

    @Test
    public void uninstalledInstancesDoNotRunCatalogueQueries()
            throws Exception
    {
        DataSource unavailable = mock(DataSource.class);
        new NavigationMigrationRunner(unavailable, mock(InstallationState.class)).run();
        verifyNoInteractions(unavailable);
    }

    @Test
    public void canonicalPathsPreserveParametersAndRemainStable()
    {
        assertEquals("/source/Src/d/MyDB", NavigationPaths.canonical("/ADMIN/SOURCE/Src/d/MyDB"));
        assertEquals("/account/profile", NavigationPaths.canonical("/admin/user/profile"));
        assertEquals("/admin/user", NavigationPaths.canonical("/system/user"));
        assertEquals("/query?sql=SELECT#editor", NavigationPaths.canonical("/admin/query?sql=SELECT#editor"));
        assertEquals("/admin/role", NavigationPaths.canonical(NavigationPaths.canonical("/system/role")));
        TreeRecord customized = TreeRecord.builder().code("MANAGEMENT").url("/workspace").i18nKey("common.source").build();
        assertEquals("common.source", NavigationPaths.normalize(customized).getI18nKey());
    }

    private String url(String code)
    {
        return jdbc.queryForObject("SELECT url FROM metaforge_menu WHERE code = ?", String.class, code);
    }

    private List<TreeRecord> flatten(List<TreeRecord> tree)
    {
        List<TreeRecord> result = new ArrayList<>();
        for (TreeRecord record : tree) {
            result.add(record);
            if (record.getChildren() != null) {
                result.addAll(flatten(record.getChildren()));
            }
        }
        return result;
    }
}
