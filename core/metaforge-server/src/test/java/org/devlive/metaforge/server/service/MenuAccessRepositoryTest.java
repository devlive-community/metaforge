package org.devlive.metaforge.server.service;

import org.devlive.metaforge.service.initializer.RoleCodeConstraint;
import org.devlive.metaforge.service.record.TreeRecord;
import org.devlive.metaforge.service.repository.RoleRepository;
import org.devlive.metaforge.service.repository.UserRepository;
import org.devlive.metaforge.service.repository.jdbc.MenuAccessRepository;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.devlive.metaforge.service.service.impl.RoleServiceImpl;
import org.devlive.metaforge.service.service.impl.UserServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.sql.DataSource;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

public class MenuAccessRepositoryTest
{
    private JdbcTemplate jdbc;
    private MenuAccessRepository menus;

    @BeforeEach
    public void initialize()
            throws Exception
    {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema/setup-schema-h2.sql"));
        }
        jdbc = new JdbcTemplate(dataSource);
        menus = new MenuAccessRepository(dataSource);
        jdbc.update("INSERT INTO metaforge_role (id, name, code) VALUES (1, 'First', 'FIRST'), (2, 'Second', 'SECOND'), (3, 'Other', 'OTHER')");
        jdbc.update("INSERT INTO metaforge_user (id, username, code) VALUES (10, 'owner', 'OWNER'), (20, 'other', 'OTHER')");
        jdbc.update("INSERT INTO metaforge_user_role_relation (user_id, role_id) VALUES (10, 1), (10, 2), (20, 3)");
        menu(100, "Admin", 0, 10, "VIEW");
        menu(200, "Home", 0, 0, "VIEW");
        menu(110, "Chosen", 100, 2, "VIEW");
        menu(120, "Sibling", 100, 1, "VIEW");
        menu(111, "Nested", 110, 1, "VIEW");
        menu(300, "Orphan", 9999, 30, "VIEW");
        menu(400, "Cycle one", 401, 40, "VIEW");
        menu(401, "Cycle two", 400, 41, "VIEW");
        menu(500, "Unsupported", 0, 50, null);
        jdbc.update("INSERT INTO metaforge_role_menu_relation (role_id, menu_id) VALUES ('1', '100'), ('1', '110'), ('2', '110'), ('2', '200'), ('3', '120')");
    }

    @AfterEach
    public void close()
    {
        SecurityContextHolder.clearContext();
        if (jdbc != null) {
            jdbc.execute("SHUTDOWN");
        }
    }

    @Test
    public void roleTreeLoadsWithoutAnyJpaSessionAndPreservesPartialAssignments()
    {
        RoleRepository roles = mock(RoleRepository.class);
        RoleCodeConstraint constraint = mock(RoleCodeConstraint.class);
        RoleServiceImpl service = new RoleServiceImpl(roles, menus, constraint);
        List<TreeRecord> tree = service.getMenusByRoleId(1L).getData();
        assertEquals(200L, tree.get(0).getId());
        TreeRecord admin = find(tree, 100);
        assertEquals(List.of(120L, 110L), admin.getChildren().stream().map(TreeRecord::getId).collect(Collectors.toList()));
        assertTrue(admin.getSelected());
        assertTrue(find(tree, 110).getSelected());
        assertFalse(find(tree, 120).getSelected());
        assertFalse(find(tree, 111).getSelected());
        assertEquals(8, flatten(tree).size());
        assertTrue(service.saveMenusByRoleId(1L, Set.of(111L)).getStatus());
        assertEquals(Set.of(111L), assigned(1));
        verifyNoInteractions(roles, constraint);
    }

    @Test
    public void userNavigationUsesSqlUnionsAndCompletesOnlyRequiredAncestors()
    {
        UserRepository users = mock(UserRepository.class);
        RoleRepository roles = mock(RoleRepository.class);
        UserServiceImpl service = new UserServiceImpl(users, roles, null, menus, null, null, null, null, null, null, null, null);
        UserDetailsService principal = new UserDetailsService(10L, "OWNER", "owner", "", List.of(), null);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, "", principal.getAuthorities()));
        List<TreeRecord> tree = service.getMenus().getData();
        assertEquals(List.of(200L, 100L), tree.stream().map(TreeRecord::getId).collect(Collectors.toList()));
        assertEquals(Set.of(100L, 110L, 200L), flatten(tree).stream().map(TreeRecord::getId).collect(Collectors.toSet()));
        menus.replaceRoleMenus(1L, Set.of(111L));
        tree = service.getMenus().getData();
        assertEquals(111L, find(tree, 110).getChildren().get(0).getId());
        assertFalse(flatten(tree).stream().anyMatch(menu -> menu.getId() == 120));
        verifyNoInteractions(users, roles);
    }

    @Test
    public void emptyAssignmentsClearOnlyTheSpecifiedRole()
    {
        assertEquals(MenuAccessRepository.SaveResult.SAVED, menus.replaceRoleMenus(1L, Set.of()));
        assertTrue(assigned(1).isEmpty());
        assertEquals(Set.of(110L, 200L), assigned(2));
        assertTrue(flatten(menus.findRoleMenus(1L).orElseThrow()).stream().noneMatch(TreeRecord::getSelected));
    }

    @Test
    public void invalidSelectionsAndMissingRolesLeaveAssignmentsUntouched()
    {
        assertEquals(MenuAccessRepository.SaveResult.INVALID_MENUS, menus.replaceRoleMenus(1L, Set.of(9999L)));
        assertEquals(MenuAccessRepository.SaveResult.INVALID_MENUS, menus.replaceRoleMenus(1L, Set.of(500L)));
        assertEquals(MenuAccessRepository.SaveResult.INVALID_MENUS, menus.replaceRoleMenus(1L, new HashSet<>(Arrays.asList(110L, null))));
        assertEquals(MenuAccessRepository.SaveResult.ROLE_NOT_FOUND, menus.replaceRoleMenus(9999L, Set.of()));
        assertTrue(menus.findRoleMenus(9999L).isEmpty());
        assertTrue(menus.findUserMenus("MISSING").isEmpty());
        assertEquals(Set.of(100L, 110L), assigned(1));
    }

    @Test
    public void failedInsertsRollBackBothDeletesAndPartialInserts()
    {
        jdbc.execute("ALTER TABLE metaforge_role_menu_relation ADD CONSTRAINT reject_nested CHECK (menu_id <> '111')");
        assertThrows(DataIntegrityViolationException.class, () -> menus.replaceRoleMenus(1L, new LinkedHashSet<>(List.of(120L, 111L))));
        assertEquals(Set.of(100L, 110L), assigned(1));
        assertEquals(Set.of(110L, 200L), assigned(2));
    }

    @Test
    public void concurrentReplacementsDoNotMixTwoAssignments()
            throws Exception
    {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        Set<Long> first = Set.of(100L, 110L);
        Set<Long> second = Set.of(200L, 120L);
        try {
            Future<MenuAccessRepository.SaveResult> one = executor.submit(() -> { barrier.await(); return menus.replaceRoleMenus(1L, first); });
            Future<MenuAccessRepository.SaveResult> two = executor.submit(() -> { barrier.await(); return menus.replaceRoleMenus(1L, second); });
            assertEquals(MenuAccessRepository.SaveResult.SAVED, one.get(10, TimeUnit.SECONDS));
            assertEquals(MenuAccessRepository.SaveResult.SAVED, two.get(10, TimeUnit.SECONDS));
            assertTrue(assigned(1).equals(first) || assigned(1).equals(second));
        }
        finally { executor.shutdownNow(); }
    }

    @Test
    public void constructingJdbcAccessDoesNotQueryAnUninstalledDatabase()
    {
        DataSource dataSource = mock(DataSource.class);
        new MenuAccessRepository(dataSource);
        verifyNoInteractions(dataSource);
    }

    private void menu(long id, String name, long parent, int sorted, String type)
    {
        jdbc.update("INSERT INTO metaforge_menu (id, name, parent, sorted, type, code, is_new, url) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                id, name, parent, sorted, type, "MENU_" + id, false, "/test/" + id);
    }

    private Set<Long> assigned(long role)
    {
        return new HashSet<>(jdbc.queryForList("SELECT menu_id FROM metaforge_role_menu_relation WHERE role_id = ?", Long.class, Long.toString(role)));
    }

    private List<TreeRecord> flatten(List<TreeRecord> tree)
    {
        List<TreeRecord> all = new ArrayList<>();
        for (TreeRecord node : tree) {
            all.add(node);
            if (node.getChildren() != null) {
                all.addAll(flatten(node.getChildren()));
            }
        }
        return all;
    }

    private TreeRecord find(List<TreeRecord> tree, long id)
    {
        return flatten(tree).stream().filter(node -> node.getId() == id).findFirst().orElseThrow();
    }
}
