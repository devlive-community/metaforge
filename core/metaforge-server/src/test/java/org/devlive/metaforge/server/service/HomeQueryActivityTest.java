package org.devlive.metaforge.server.service;

import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.service.activity.QueryActivity;
import org.devlive.metaforge.service.initializer.InitializerConfigure;
import org.devlive.metaforge.service.repository.PluginAuditRepository;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.devlive.metaforge.service.service.SystemSettingService;
import org.devlive.metaforge.service.service.impl.PluginAuditServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/** The home charts use the same user, calendar window and real query records. */
public class HomeQueryActivityTest
{
    private JdbcTemplate jdbc;
    private PluginAuditServiceImpl service;
    private LocalDate today;

    @BeforeEach
    public void setup()
    {
        DriverManagerDataSource database = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(database);
        jdbc.execute("CREATE TABLE metaforge_source (id BIGINT PRIMARY KEY, name VARCHAR(100), user_id BIGINT)");
        jdbc.execute("CREATE TABLE metaforge_source_query (user_id BIGINT, source_id BIGINT, create_time TIMESTAMP)");
        jdbc.update("INSERT INTO metaforge_source VALUES (10, 'Owned', 1), (20, 'Shared', 2)");
        service = new PluginAuditServiceImpl(mock(PluginAuditRepository.class), mock(InitializerConfigure.class), mock(PluginManager.class),
                new NamedParameterJdbcTemplate(database), mock(SystemSettingService.class));
        today = LocalDate.now();
        signIn(1L);
    }

    @AfterEach
    public void clear()
    {
        SecurityContextHolder.clearContext();
        jdbc.execute("SHUTDOWN");
    }

    private static void signIn(Long id)
    {
        UserDetailsService user = new UserDetailsService(id, "USER", "user", "", List.of(), null);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, "", user.getAuthorities()));
    }

    private void query(long user, long source, int daysAgo)
    {
        jdbc.update("INSERT INTO metaforge_source_query VALUES (?, ?, ?)", user, source, Timestamp.valueOf(today.minusDays(daysAgo).atStartOfDay()));
    }

    @Test
    public void bothChartsIncludeOwnQueriesOnSharedAndDeletedSourcesButExcludeOtherUsers()
    {
        query(1, 10, 0);
        query(1, 20, 6);
        query(1, 999, 2);
        query(2, 10, 0);
        query(2, 20, 1);
        query(1, 10, 7);
        query(1, 10, -1);
        QueryActivity activity = service.getHomeActivity(7).getData();
        assertEquals(7, activity.getDays());
        assertEquals(3, activity.getTotal());
        assertEquals(7, activity.getTrend().size());
        assertEquals(today.minusDays(6).toString(), activity.getTrend().get(0).getDate());
        assertEquals(today.toString(), activity.getTrend().get(6).getDate());
        assertEquals(0, activity.getTrend().get(1).getCount(), "missing days stay visible as zero");
        assertEquals(1, activity.getTrend().get(0).getCount(), "the inclusive start boundary");
        assertEquals(3, activity.getTrend().stream().mapToLong(QueryActivity.Day::getCount).sum());
        assertEquals(3, activity.getSources().stream().mapToLong(QueryActivity.Source::getCount).sum());
        assertEquals(List.of("", "Owned", "Shared"), activity.getSources().stream().map(QueryActivity.Source::getName).toList());
    }

    @Test
    public void thirtyDaysHasTheSameBoundariesAndCountsAsItsDistribution()
    {
        query(1, 10, 0);
        query(1, 10, 29);
        query(1, 20, 8);
        query(1, 20, 30);
        QueryActivity activity = service.getHomeActivity(30).getData();
        assertEquals(30, activity.getTrend().size());
        assertEquals(3, activity.getTotal());
        assertEquals("Owned", activity.getSources().get(0).getName());
        assertEquals(2, activity.getSources().get(0).getCount());
        assertEquals(3, activity.getTrend().stream().mapToLong(QueryActivity.Day::getCount).sum());
        assertEquals(3, activity.getSources().stream().mapToLong(QueryActivity.Source::getCount).sum());
        assertEquals(1, service.getHomeActivity(7).getData().getTotal());
    }

    @Test
    public void emptyAndUnsupportedPeriodsNeverInventData()
    {
        QueryActivity activity = service.getHomeActivity(7).getData();
        assertEquals(0, activity.getTotal());
        assertTrue(activity.getSources().isEmpty());
        assertTrue(activity.getTrend().stream().allMatch(day -> day.getCount() == 0));
        assertFalse(service.getHomeActivity(365).getStatus());
        assertFalse(service.getHomeActivity(-1).getStatus());
        signIn(null);
        assertFalse(service.getHomeActivity(7).getStatus());
    }
}
