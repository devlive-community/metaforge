package org.devlive.metaforge.server.service;

import org.devlive.metaforge.common.enums.ServiceState;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.entity.MenuEntity;
import org.devlive.metaforge.service.entity.RoleEntity;
import org.devlive.metaforge.service.initializer.RoleCodeConstraint;
import org.devlive.metaforge.service.repository.RoleRepository;
import org.devlive.metaforge.service.service.impl.RoleServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

import java.sql.SQLException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class RoleCodeTest
{
    private final RoleRepository repository = mock(RoleRepository.class);
    private final RoleCodeConstraint constraint = mock(RoleCodeConstraint.class);
    private final RoleServiceImpl service = new RoleServiceImpl(repository, null, constraint, null);

    @Test
    public void customCodeIsTrimmedAndSavedAsANewRole()
            throws Exception
    {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        CommonResponse<RoleEntity> response = service.createRole(role(null, " CUSTOM_READ "));
        assertTrue(response.getStatus());
        assertEquals("CUSTOM_READ", response.getData().getCode());
        assertEquals("Test role", response.getData().getName());
        verify(constraint).ensure();
    }

    @Test
    public void blankCodeStillGeneratesAnIdentifier()
    {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        CommonResponse<RoleEntity> response = service.createRole(role(null, "   "));
        assertTrue(response.getStatus());
        assertTrue(response.getData().getCode().matches("[a-f0-9]{32}"));
    }

    @Test
    public void invalidDuplicateAndExistingIdentityCannotBeCreated()
    {
        assertEquals(ServiceState.USER_ROLE_CODE_INVALID.getCode(), service.createRole(role(null, "not valid")).getCode());
        assertEquals(ServiceState.USER_ROLE_CODE_INVALID.getCode(), service.createRole(role(null, "A".repeat(65))).getCode());
        assertEquals(ServiceState.USER_ROLE_ID_MISMATCH.getCode(), service.createRole(role(1L, "EXISTING")).getCode());
        when(repository.existsByCode("TAKEN")).thenReturn(true);
        assertEquals(ServiceState.USER_ROLE_CODE_EXISTS.getCode(), service.createRole(role(null, "TAKEN")).getCode());
        verify(repository, never()).save(any());
        verifyNoInteractions(constraint);
    }

    @Test
    public void checkEndpointReportsAvailabilityAndRejectsInvalidFormats()
    {
        when(repository.existsByCode("TAKEN")).thenReturn(true);
        assertTrue(service.checkCode("AVAILABLE").getData());
        assertFalse(service.checkCode(" TAKEN ").getData());
        assertTrue(service.checkCode("").getData());
        assertEquals(ServiceState.USER_ROLE_CODE_INVALID.getCode(), service.checkCode("bad/code").getCode());
        verifyNoInteractions(constraint);
    }

    @Test
    public void databaseUniquenessRaceReturnsADuplicateResponse()
    {
        when(repository.existsByCode("RACE")).thenReturn(false, true);
        when(repository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate code"));
        assertEquals(ServiceState.USER_ROLE_CODE_EXISTS.getCode(), service.createRole(role(null, "RACE")).getCode());
    }

    @Test
    public void editingRejectsCodeChangesAndMismatchedIdentities()
    {
        RoleEntity existing = role(1L, "ORIGINAL");
        when(repository.findByCode("ORIGINAL")).thenReturn(Optional.of(existing));
        assertEquals(ServiceState.USER_ROLE_CODE_IMMUTABLE.getCode(), service.updateRole("ORIGINAL", role(1L, "OTHER")).getCode());
        assertEquals(ServiceState.USER_ROLE_ID_MISMATCH.getCode(), service.updateRole("ORIGINAL", role(2L, "ORIGINAL")).getCode());
        assertEquals("ORIGINAL", existing.getCode());
        verify(repository, never()).save(any());
    }

    @Test
    public void validEditsPreserveTheCodeAndAssignedMenus()
    {
        RoleEntity existing = role(1L, "ORIGINAL");
        MenuEntity menu = new MenuEntity();
        menu.setId(10L);
        Set<MenuEntity> menus = Set.of(menu);
        existing.setMenus(menus);
        when(repository.findByCode("ORIGINAL")).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        RoleEntity update = role(1L, "ORIGINAL");
        update.setName("Updated role");
        CommonResponse<RoleEntity> response = service.updateRole("ORIGINAL", update);
        assertTrue(response.getStatus());
        assertEquals("Updated role", response.getData().getName());
        assertEquals("ORIGINAL", response.getData().getCode());
        assertSame(menus, response.getData().getMenus());
    }

    @Test
    public void legacyDatabasesGainAnIdempotentUniqueIndex()
            throws Exception
    {
        for (boolean lowerCase : new boolean[] {true, false}) {
            DriverManagerDataSource dataSource = database(lowerCase);
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            jdbc.update("INSERT INTO metaforge_role (code) VALUES ('CODE')");
            RoleCodeConstraint guard = new RoleCodeConstraint(dataSource);
            guard.ensure();
            guard.ensure();
            assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("INSERT INTO metaforge_role (code) VALUES ('CODE')"));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM metaforge_role", Integer.class));
            jdbc.execute("SHUTDOWN");
        }
    }

    @Test
    public void existingDuplicatesAreReportedWithoutChangingStoredData()
    {
        DriverManagerDataSource dataSource = database(true);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO metaforge_role (code) VALUES ('DUPLICATE'), ('DUPLICATE')");
        assertThrows(SQLException.class, () -> new RoleCodeConstraint(dataSource).ensure());
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM metaforge_role", Integer.class));
        jdbc.execute("SHUTDOWN");
    }

    @Test
    public void constructingTheGuardDoesNotQueryAnUninstalledDatabase()
    {
        DataSource dataSource = mock(DataSource.class);
        new RoleCodeConstraint(dataSource);
        verifyNoInteractions(dataSource);
    }

    private DriverManagerDataSource database(boolean lowerCase)
    {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=" + lowerCase, "sa", "");
        new JdbcTemplate(dataSource).execute("CREATE TABLE metaforge_role (id BIGINT AUTO_INCREMENT PRIMARY KEY, code VARCHAR(100))");
        return dataSource;
    }

    private RoleEntity role(Long id, String code)
    {
        RoleEntity role = new RoleEntity();
        role.setId(id);
        role.setCode(code);
        role.setName("Test role");
        role.setDescription("Test description");
        return role;
    }
}
