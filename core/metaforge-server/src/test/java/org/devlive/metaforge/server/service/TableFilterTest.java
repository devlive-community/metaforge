package org.devlive.metaforge.server.service;

import org.devlive.metaforge.common.enums.State;
import org.devlive.metaforge.service.body.FilterBody;
import org.devlive.metaforge.service.body.FilterCriterionBody;
import org.devlive.metaforge.service.entity.SourceEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.entity.UserLogEntity;
import org.devlive.metaforge.service.repository.SourceRepository;
import org.devlive.metaforge.service.repository.UserLogRepository;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.devlive.metaforge.service.service.impl.UserLogServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.persistence.EntityManager;

import java.sql.Connection;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TableFilterTest
{
    private LocalContainerEntityManagerFactoryBean factory;
    private EntityManager manager;
    private SourceRepository repository;
    private UserEntity owner;
    private UserEntity other;

    @BeforeEach
    public void initialize()
            throws Exception
    {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema/setup-schema-h2.sql"));
        }
        factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setPackagesToScan("org.devlive.metaforge.service.entity");
        factory.afterPropertiesSet();
        manager = factory.getObject().createEntityManager();
        repository = new JpaRepositoryFactory(manager).getRepository(SourceRepository.class);
        manager.getTransaction().begin();
        owner = user("owner");
        other = user("other");
        for (int day = 1; day <= 4; day++) {
            source("owned-" + day, "Alpha " + day, day % 2 == 1, "2026-10-0" + day + "T12:00:00", owner, false);
        }
        source("public", "Alpha shared", false, "2026-10-03T23:59:59", other, true);
        source("private", "Alpha private", true, "2026-10-03T12:00:00", other, false);
        manager.getTransaction().commit();
        manager.clear();
    }

    @AfterEach
    public void close()
    {
        SecurityContextHolder.clearContext();
        if (manager != null) {
            if (manager.getTransaction().isActive()) {
                manager.getTransaction().rollback();
            }
            manager.close();
        }
        if (factory != null) {
            factory.destroy();
        }
    }

    @Test
    public void keywordFiltersBeforePaginationAndKeepsPrivateSourcesHidden()
    {
        FilterBody filter = new FilterBody();
        filter.setKeyword(" aLPHa ");
        filter.setPage(2);
        filter.setSize(2);
        Page<SourceEntity> page = repository.findFiltered(filter, owner);
        assertEquals(5, page.getTotalElements());
        assertEquals(2, page.getNumberOfElements());
        assertTrue(page.getContent().stream().noneMatch(source -> "private".equals(source.getCode())));
        filter.setCode("private");
        assertEquals(0, repository.findFiltered(filter, owner).getTotalElements());
    }

    @Test
    public void combinesStatusCodeAndInclusiveDateRange()
    {
        FilterBody filter = new FilterBody();
        filter.setCreatedFrom("2026-10-02");
        filter.setCreatedTo("2026-10-03");
        assertEquals(3, repository.findFiltered(filter, owner).getTotalElements());
        filter.setActive(false);
        assertEquals(2, repository.findFiltered(filter, owner).getTotalElements());
        filter.setCode("public");
        assertEquals(1, repository.findFiltered(filter, owner).getTotalElements());
    }

    @Test
    public void treatsLikeWildcardsAndSqlTextAsLiteralSearchTerms()
    {
        manager.getTransaction().begin();
        source("literal", "literal%_\\value", true, "2026-10-03T12:00:00", owner, false);
        source("ordinary", "literal other value", true, "2026-10-03T12:00:00", owner, false);
        manager.getTransaction().commit();
        FilterBody filter = new FilterBody();
        filter.setKeyword("%_\\");
        assertEquals(1, repository.findFiltered(filter, owner).getTotalElements());
        filter.setKeyword("' OR 1=1");
        assertEquals(0, repository.findFiltered(filter, owner).getTotalElements());
    }

    @Test
    public void emptyCriteriaRetainOriginalVisibilityAndInvalidDatesAreRejected()
    {
        FilterBody filter = new FilterBody();
        assertEquals(5, repository.findFiltered(filter, owner).getTotalElements());
        filter.setCreatedFrom("2026-10-04");
        filter.setCreatedTo("2026-10-01");
        assertThrows(RuntimeException.class, () -> repository.findFiltered(filter, owner));
        filter.setCreatedFrom("invalid");
        assertThrows(RuntimeException.class, () -> repository.findFiltered(filter, owner));
    }

    @Test
    public void businessConditionsUseTypedValuesAndRejectUnsupportedFields()
    {
        FilterBody filter = new FilterBody();
        filter.setConditions(Map.of("type", new FilterCriterionBody("H2", true), "host", new FilterCriterionBody("local", false)));
        assertEquals(5, repository.findFiltered(filter, owner).getTotalElements());
        filter.setConditions(Map.of("available", new FilterCriterionBody("false", true)));
        assertEquals(3, repository.findFiltered(filter, owner).getTotalElements());
        filter.setConditions(Map.of("password", new FilterCriterionBody("secret", false)));
        assertThrows(RuntimeException.class, () -> repository.findFiltered(filter, owner));
        filter.setConditions(Map.of("available", new FilterCriterionBody("invalid", true)));
        assertThrows(RuntimeException.class, () -> repository.findFiltered(filter, owner));
    }

    @Test
    public void loginFiltersUseEnumValuesAndPreserveOwnerScope()
    {
        manager.getTransaction().begin();
        log(owner, State.SUCCESS, "10.0.0.1");
        log(owner, State.FAILURE, "10.0.0.2");
        log(other, State.SUCCESS, "10.0.0.1");
        manager.getTransaction().commit();
        UserDetailsService principal = UserDetailsService.build(owner);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, "", principal.getAuthorities()));
        UserLogRepository logs = new JpaRepositoryFactory(manager).getRepository(UserLogRepository.class);
        UserLogServiceImpl service = new UserLogServiceImpl(logs);
        FilterBody filter = new FilterBody();
        filter.setConditions(Map.of("state", new FilterCriterionBody("SUCCESS", true), "ip", new FilterCriterionBody("10.0.0", false)));
        filter.setCreatedFrom("2026-10-03");
        filter.setCreatedTo("2026-10-03");
        assertEquals(1, service.getAllByFilter(filter).getData().getTotal());
        filter.setConditions(Map.of("state", new FilterCriterionBody("INVALID", true)));
        assertThrows(RuntimeException.class, () -> service.getAllByFilter(filter));
    }

    private void log(UserEntity user, State state, String ip)
    {
        UserLogEntity log = new UserLogEntity();
        log.setUser(user);
        log.setState(state);
        log.setIp(ip);
        log.setCreateTime(Timestamp.valueOf("2026-10-03 12:00:00"));
        manager.persist(log);
    }

    private UserEntity user(String username)
    {
        UserEntity user = new UserEntity();
        user.setUsername(username);
        manager.persist(user);
        return user;
    }

    private void source(String code, String name, boolean active, String created, UserEntity user, boolean publish)
    {
        SourceEntity source = new SourceEntity();
        source.setCode(code);
        source.setName(name);
        source.setType("H2");
        source.setHost("localhost");
        source.setProtocol("JDBC");
        source.setActive(active);
        source.setAvailable(active);
        source.setCreateTime(Date.from(LocalDateTime.parse(created).atZone(ZoneId.systemDefault()).toInstant()));
        source.setUser(user);
        source.setPublish(publish);
        manager.persist(source);
    }
}
