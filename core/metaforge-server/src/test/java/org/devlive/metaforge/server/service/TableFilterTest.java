package org.devlive.metaforge.server.service;

import org.devlive.metaforge.common.enums.State;
import org.devlive.metaforge.server.support.PersistenceContext;
import org.devlive.metaforge.server.support.TestDatabase;
import org.devlive.metaforge.service.body.FilterBody;
import org.devlive.metaforge.service.body.FilterCriterionBody;
import org.devlive.metaforge.service.entity.SourceEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.entity.UserLogEntity;
import org.devlive.metaforge.service.persistence.Persistence;
import org.devlive.metaforge.service.repository.SourceRepository;
import org.devlive.metaforge.service.repository.UserLogRepository;
import org.devlive.metaforge.service.repository.UserRepository;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.devlive.metaforge.service.service.impl.UserLogServiceImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Listing filters on the database selected with {@code -Dmetaforge.it.database}.
 */
public class TableFilterTest
{
    private static PersistenceContext context;
    private SourceRepository repository;
    private UserEntity owner;
    private UserEntity other;

    @BeforeAll
    public static void startDatabase()
            throws Exception
    {
        context = PersistenceContext.start(TestDatabase.selectedSpec());
    }

    @AfterAll
    public static void stopDatabase()
    {
        context.close();
    }

    @BeforeEach
    public void initialize()
    {
        context.jdbc().update("DELETE FROM metaforge_user_log");
        context.jdbc().update("DELETE FROM metaforge_source");
        context.jdbc().update("DELETE FROM metaforge_user");
        repository = context.bean(SourceRepository.class);
        owner = user("owner");
        other = user("other");
        for (int day = 1; day <= 4; day++) {
            source("owned-" + day, "Alpha " + day, day % 2 != 0, "2026-10-0" + day + "T12:00:00", owner, false);
        }
        source("public", "Alpha shared", false, "2026-10-03T23:59:59", other, true);
        source("private", "Alpha private", true, "2026-10-03T12:00:00", other, false);
    }

    @AfterEach
    public void close()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void keywordFiltersBeforePaginationAndKeepsPrivateSourcesHidden()
    {
        FilterBody filter = new FilterBody();
        filter.setKeyword(" aLPHa ");
        filter.setPage(2);
        filter.setSize(2);
        Page<SourceEntity> page = Persistence.filtered(repository, filter, owner);
        assertEquals(5, page.getTotalElements());
        assertEquals(2, page.getNumberOfElements());
        assertTrue(page.getContent().stream().noneMatch(source -> "private".equals(source.getCode())));
        filter.setCode("private");
        assertEquals(0, Persistence.filtered(repository, filter, owner).getTotalElements());
    }

    @Test
    public void combinesStatusCodeAndInclusiveDateRange()
    {
        FilterBody filter = new FilterBody();
        filter.setCreatedFrom("2026-10-02");
        filter.setCreatedTo("2026-10-03");
        assertEquals(3, Persistence.filtered(repository, filter, owner).getTotalElements());
        filter.setActive(false);
        assertEquals(2, Persistence.filtered(repository, filter, owner).getTotalElements());
        filter.setCode("public");
        assertEquals(1, Persistence.filtered(repository, filter, owner).getTotalElements());
    }

    @Test
    public void treatsLikeWildcardsAndSqlTextAsLiteralSearchTerms()
    {
        source("literal", "literal%_\\value", true, "2026-10-03T12:00:00", owner, false);
        source("ordinary", "literal other value", true, "2026-10-03T12:00:00", owner, false);
        FilterBody filter = new FilterBody();
        filter.setKeyword("%_\\");
        assertEquals(1, Persistence.filtered(repository, filter, owner).getTotalElements());
        filter.setKeyword("' OR 1=1");
        assertEquals(0, Persistence.filtered(repository, filter, owner).getTotalElements());
    }

    @Test
    public void emptyCriteriaRetainOriginalVisibilityAndInvalidDatesAreRejected()
    {
        FilterBody filter = new FilterBody();
        assertEquals(5, Persistence.filtered(repository, filter, owner).getTotalElements());
        filter.setCreatedFrom("2026-10-04");
        filter.setCreatedTo("2026-10-01");
        assertThrows(RuntimeException.class, () -> Persistence.filtered(repository, filter, owner));
        filter.setCreatedFrom("invalid");
        assertThrows(RuntimeException.class, () -> Persistence.filtered(repository, filter, owner));
    }

    @Test
    public void businessConditionsUseTypedValuesAndRejectUnsupportedFields()
    {
        FilterBody filter = new FilterBody();
        filter.setConditions(Map.of("type", new FilterCriterionBody("H2", true), "host", new FilterCriterionBody("local", false)));
        assertEquals(5, Persistence.filtered(repository, filter, owner).getTotalElements());
        filter.setConditions(Map.of("available", new FilterCriterionBody("false", true)));
        assertEquals(3, Persistence.filtered(repository, filter, owner).getTotalElements());
        filter.setConditions(Map.of("password", new FilterCriterionBody("secret", false)));
        assertThrows(RuntimeException.class, () -> Persistence.filtered(repository, filter, owner));
        filter.setConditions(Map.of("available", new FilterCriterionBody("invalid", true)));
        assertThrows(RuntimeException.class, () -> Persistence.filtered(repository, filter, owner));
    }

    @Test
    public void loginFiltersUseEnumValuesAndPreserveOwnerScope()
    {
        log(owner, State.SUCCESS, "10.0.0.1");
        log(owner, State.FAILURE, "10.0.0.2");
        log(other, State.SUCCESS, "10.0.0.1");
        owner.setRoles(new HashSet<>());
        UserDetailsService principal = UserDetailsService.build(owner);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, "", principal.getAuthorities()));
        UserLogServiceImpl service = new UserLogServiceImpl(context.bean(UserLogRepository.class));
        FilterBody filter = new FilterBody();
        filter.setConditions(Map.of("state", new FilterCriterionBody("SUCCESS", true), "ip", new FilterCriterionBody("10.0.0", false)));
        filter.setCreatedFrom("2026-10-03");
        filter.setCreatedTo("2026-10-03");
        assertEquals(1, service.getAllByFilter(filter).getData().getTotal());
        filter.setConditions(Map.of("state", new FilterCriterionBody("INVALID", true)));
        assertThrows(RuntimeException.class, () -> service.getAllByFilter(filter));
    }

    @Test
    public void listingsLoadReferencedOwnersInOneBatch()
    {
        FilterBody filter = new FilterBody();
        filter.setSize(50);
        Page<SourceEntity> page = Persistence.filtered(repository, filter, owner);
        assertTrue(page.getContent().stream().allMatch(source -> source.getUser() != null && source.getUser().getId().equals(source.getUserId())));
    }

    private void log(UserEntity user, State state, String ip)
    {
        UserLogEntity log = new UserLogEntity();
        log.setUser(user);
        log.setState(state);
        log.setIp(ip);
        log.setCreateTime(Timestamp.valueOf("2026-10-03 12:00:00"));
        context.bean(UserLogRepository.class).save(log);
    }

    private UserEntity user(String username)
    {
        UserEntity user = new UserEntity();
        user.setUsername(username);
        return context.bean(UserRepository.class).save(user);
    }

    private void source(String code, String name, boolean active, String created, UserEntity user, boolean publish)
    {
        SourceEntity source = new SourceEntity();
        source.setCode(code);
        source.setName(name);
        source.setType("H2");
        source.setHost("localhost");
        source.setPort(0);
        source.setProtocol("JDBC");
        source.setActive(active);
        source.setAvailable(active);
        source.setUser(user);
        source.setPublish(publish);
        repository.save(source);
        // auditing stamps the creation time on insert; fixtures need fixed dates
        context.jdbc().update("UPDATE metaforge_source SET create_time = ? WHERE code = ?", Timestamp.valueOf(LocalDateTime.parse(created)), code);
    }
}
