package org.devlive.metaforge.server.service;

import org.devlive.metaforge.service.entity.DashboardEntity;
import org.devlive.metaforge.service.entity.ReportEntity;
import org.devlive.metaforge.service.repository.DashboardRepository;
import org.devlive.metaforge.service.repository.ReportRepository;
import org.devlive.metaforge.service.repository.jdbc.LinkRepository;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.devlive.metaforge.service.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class DashboardAccessTest
{
    private final DashboardRepository dashboards = mock(DashboardRepository.class);
    private final ReportRepository reports = mock(ReportRepository.class);
    private final LinkRepository links = mock(LinkRepository.class);
    private final DashboardServiceImpl service = new DashboardServiceImpl(dashboards, reports, links);

    @BeforeEach
    public void authenticate()
    {
        UserDetailsService user = new UserDetailsService(1L, "USER", "owner", "", List.of(), null);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, "", user.getAuthorities()));
    }

    @AfterEach
    public void clear()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void cannotOverwriteOrDeleteAnotherOwnersDashboard()
    {
        DashboardEntity dashboard = DashboardEntity.builder().id(5L).code("DASH").userId(2L).build();
        when(dashboards.findByCode("DASH")).thenReturn(Optional.of(dashboard));
        when(dashboards.findById(5L)).thenReturn(Optional.of(dashboard));

        assertFalse(service.saveOrUpdate(dashboards, DashboardEntity.builder().id(5L).build()).getStatus());
        assertFalse(service.getByCode(dashboards, "DASH").getStatus());
        assertFalse(service.deleteByCode(dashboards, "DASH").getStatus());
        verify(dashboards, never()).save(any());
        verify(dashboards, never()).delete(any());
        verifyNoInteractions(reports, links);
    }

    @Test
    public void onlyOwnedReportsCanBeLinked()
    {
        ReportEntity requested = ReportEntity.builder().code("CHART").build();
        DashboardEntity draft = DashboardEntity.builder().reports(Set.of(requested)).build();
        when(reports.findByCode("CHART")).thenReturn(Optional.of(ReportEntity.builder().id(8L).userId(2L).build()));
        assertFalse(service.saveOrUpdate(dashboards, draft).getStatus());
        verify(dashboards, never()).save(any());
        verifyNoInteractions(links);

        when(reports.findByCode("CHART")).thenReturn(Optional.of(ReportEntity.builder().id(9L).userId(1L).build()));
        when(dashboards.save(draft)).thenReturn(DashboardEntity.builder().id(7L).build());
        assertTrue(service.saveOrUpdate(dashboards, draft).getStatus());
        verify(links).replaceDashboardReports(7L, List.of(9L));
    }
}
