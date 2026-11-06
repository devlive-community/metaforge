package org.devlive.metaforge.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.server.support.PersistenceContext;
import org.devlive.metaforge.service.body.DashboardShareBody;
import org.devlive.metaforge.service.dashboard.share.DashboardViewerDocument;
import org.devlive.metaforge.service.entity.DashboardEntity;
import org.devlive.metaforge.service.entity.ReportEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.enums.SharePermission;
import org.devlive.metaforge.service.repository.DashboardRepository;
import org.devlive.metaforge.service.repository.ReportRepository;
import org.devlive.metaforge.service.repository.RoleRepository;
import org.devlive.metaforge.service.repository.UserRepository;
import org.devlive.metaforge.service.repository.jdbc.DashboardShareRepository;
import org.devlive.metaforge.service.repository.jdbc.LinkRepository;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.devlive.metaforge.service.service.ReportService;
import org.devlive.metaforge.service.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Sharing data screens: who sees what, what they may do, and what reaches them. */
public class DashboardShareTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();
    // Owner user 2 publishes a screen with a container (row) holding a chart, and a separate table.
    private static final String SCREEN = "{\"version\":3,"
            + "\"draft\":{\"mode\":\"wall\",\"boards\":{\"wall\":{\"widgets\":[{\"id\":\"secret\",\"reportCode\":\"DRAFT_ONLY\"}]}}},"
            + "\"published\":{\"mode\":\"wall\",\"boards\":{\"wall\":{\"widgets\":["
            + "{\"id\":\"row\"},{\"id\":\"chart\",\"parentId\":\"row\",\"reportCode\":\"SALES\"},{\"id\":\"table\",\"reportCode\":\"COSTS\"}]}}}}";

    private final DashboardRepository dashboards = mock(DashboardRepository.class);
    private final ReportRepository reportRepository = mock(ReportRepository.class);
    private final LinkRepository links = mock(LinkRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final DashboardShareRepository shares = mock(DashboardShareRepository.class);
    private final ReportService reports = mock(ReportService.class);
    private final DashboardServiceImpl service = new DashboardServiceImpl(dashboards, reportRepository, links);
    private final DashboardEntity screen = DashboardEntity.builder().id(9L).code("SCREEN").name("Sales").userId(2L).configure(SCREEN).build();

    @BeforeEach
    public void setup()
    {
        service.setUsers(users);
        service.setRoles(roles);
        service.setShares(shares);
        service.setReports(reports);
        signIn(1L);
        when(dashboards.findByCode("SCREEN")).thenReturn(Optional.of(screen));
        when(dashboards.findById(9L)).thenReturn(Optional.of(screen));
        when(links.reportIdsOfDashboard(9L)).thenReturn(List.of(31L, 32L));
        when(links.roleIdsOfUser(1L)).thenReturn(List.of(7L));
        ReportEntity sales = ReportEntity.builder().id(31L).code("SALES").userId(2L).build();
        ReportEntity costs = ReportEntity.builder().id(32L).code("COSTS").userId(2L).build();
        when(reportRepository.findAllById(any())).thenReturn(List.of(sales, costs));
        when(reportRepository.findByCode("SALES")).thenReturn(Optional.of(sales));
        when(reportRepository.findByCode("COSTS")).thenReturn(Optional.of(costs));
        UserEntity owner = new UserEntity();
        owner.setId(2L);
        owner.setUsername("owner");
        when(users.findById(2L)).thenReturn(Optional.of(owner));
    }

    @AfterEach
    public void clear()
    {
        SecurityContextHolder.clearContext();
    }

    private static void signIn(Long id)
    {
        UserDetailsService user = new UserDetailsService(id, "USER", "user" + id, "", List.of(), null);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, "", user.getAuthorities()));
    }

    private void grantRole(String permission, String widgets)
    {
        DashboardShareRepository.Share share = new DashboardShareRepository.Share();
        share.setDashboardId(9L);
        share.setWidgets(widgets);
        when(shares.find(9L)).thenReturn(Optional.of(share));
        when(shares.members(9L)).thenReturn(List.of(new DashboardShareRepository.Member(DashboardShareRepository.ROLE, 7L, permission)));
    }

    @Test
    public void viewersGetOnlyThePublishedChosenWidgetsAndTheirReports() throws Exception
    {
        grantRole("VIEW", "[\"row\"]");
        CommonResponse<DashboardEntity> response = service.getByCode(dashboards, "SCREEN");
        assertTrue(response.getStatus());
        DashboardEntity seen = response.getData();
        assertEquals("VIEW", seen.getPermission());
        assertEquals("owner", seen.getOwnerName());
        JsonNode document = MAPPER.readTree(seen.getConfigure());
        // The draft never reaches a viewer: both parts hold the published design.
        assertEquals(document.get("published"), document.get("draft"));
        assertFalse(seen.getConfigure().contains("secret"));
        List<String> ids = document.path("published").path("boards").path("wall").findValuesAsText("id");
        assertEquals(List.of("row", "chart"), ids, "the chosen container keeps its child");
        assertEquals(Set.of("SALES"), seen.getReports().stream().map(ReportEntity::getCode).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    public void editorsSeeTheWholeScreenAndSaveDraftsWithoutChangingOwnerOrPublishedVersion() throws Exception
    {
        grantRole("EDIT", "[\"row\"]");
        DashboardEntity loaded = service.getByCode(dashboards, "SCREEN").getData();
        assertEquals("EDIT", loaded.getPermission());
        assertEquals(SCREEN, loaded.getConfigure());

        String edited = SCREEN.replace("\"mode\":\"wall\",\"boards\":{\"wall\":{\"widgets\":[{\"id\":\"secret\"", "\"mode\":\"wall\",\"boards\":{\"wall\":{\"widgets\":[{\"id\":\"draft2\"")
                .replace("{\"id\":\"table\",\"reportCode\":\"COSTS\"}", "{\"id\":\"changed\"}");
        DashboardEntity draft = DashboardEntity.builder().id(9L).name("Sales").configure(edited).reports(Set.of(ReportEntity.builder().code("SALES").build())).build();
        AtomicReference<DashboardEntity> saved = new AtomicReference<>();
        when(dashboards.save(any())).thenAnswer(call -> {
            saved.set(call.getArgument(0));
            return call.getArgument(0);
        });
        assertTrue(service.saveOrUpdate(dashboards, draft).getStatus());
        assertEquals(2L, saved.get().getUserId(), "the owner stays the owner");
        JsonNode stored = MAPPER.readTree(saved.get().getConfigure());
        assertTrue(stored.path("draft").toString().contains("draft2"));
        assertTrue(stored.path("published").toString().contains("COSTS"), "editors cannot change the published screen");
        assertFalse(stored.path("published").toString().contains("changed"));
    }

    @Test
    public void publishersPublishWhileViewersAndStrangersCannotChangeAnything()
    {
        grantRole("PUBLISH", null);
        when(dashboards.save(any())).thenAnswer(call -> call.getArgument(0));
        String republished = SCREEN.replace("{\"id\":\"table\",\"reportCode\":\"COSTS\"}", "{\"id\":\"changed\"}");
        DashboardEntity draft = DashboardEntity.builder().id(9L).name("Sales").configure(republished).build();
        assertTrue(service.saveOrUpdate(dashboards, draft).getData().getConfigure().contains("changed"));

        grantRole("VIEW", null);
        assertFalse(service.saveOrUpdate(dashboards, DashboardEntity.builder().id(9L).configure(SCREEN).build()).getStatus());
        assertFalse(service.deleteByCode(dashboards, "SCREEN").getStatus());
        assertFalse(service.getShare("SCREEN").getStatus());
        assertFalse(service.saveShare("SCREEN", new DashboardShareBody()).getStatus());

        when(shares.members(9L)).thenReturn(List.of());
        when(shares.find(9L)).thenReturn(Optional.empty());
        assertFalse(service.getByCode(dashboards, "SCREEN").getStatus(), "not shared at all");
        verify(dashboards, never()).delete(any());
    }

    @Test
    public void viewersRefreshOnlyVisibleChartsAndTheyRunAsTheOwner()
    {
        grantRole("VIEW", "[\"table\"]");
        AtomicReference<Long> runAs = new AtomicReference<>();
        when(reports.refreshChart("COSTS")).thenAnswer(call -> {
            runAs.set(UserDetailsService.getUser().getId());
            return CommonResponse.success(List.of(Map.of("value", 1)));
        });
        assertFalse(service.refreshChart("SCREEN", "SALES").getStatus(), "a hidden widget's report stays closed");
        assertTrue(service.refreshChart("SCREEN", "COSTS").getStatus());
        assertEquals(2L, runAs.get());
        assertEquals(1L, UserDetailsService.getUser().getId(), "the viewer's identity is restored");
    }

    @Test
    public void ownersShareWithExistingMembersAndPublishedWidgetsOnly()
    {
        signIn(2L);
        when(users.existsById(5L)).thenReturn(true);
        when(roles.existsById(7L)).thenReturn(true);
        when(shares.find(9L)).thenReturn(Optional.empty());
        when(shares.members(9L)).thenReturn(List.of());

        DashboardShareBody body = new DashboardShareBody();
        body.setEveryone("view");
        body.setMembers(List.of(new DashboardShareBody.Member("USER", 5L, null, "EDIT"), new DashboardShareBody.Member("ROLE", 7L, null, "PUBLISH")));
        body.setWidgets(List.of("chart", "secret"));
        assertTrue(service.saveShare("SCREEN", body).getStatus(), "unknown widget ids are dropped");
        verify(shares).save(org.mockito.ArgumentMatchers.argThat(share -> "VIEW".equals(share.getEveryonePermission())
                && "[\"chart\"]".equals(share.getWidgets()) && share.getPublicToken() != null));
        verify(shares).replaceMembers(org.mockito.ArgumentMatchers.eq(9L), org.mockito.ArgumentMatchers.argThat(members -> members.size() == 2));

        body.setWidgets(List.of("secret"));
        assertFalse(service.saveShare("SCREEN", body).getStatus(), "draft-only widgets cannot be shared");
        body.setWidgets(null);
        body.setMembers(List.of(new DashboardShareBody.Member("USER", 404L, null, "VIEW")));
        assertFalse(service.saveShare("SCREEN", body).getStatus());
        body.setMembers(List.of(new DashboardShareBody.Member("USER", 5L, null, "OWNER")));
        assertFalse(service.saveShare("SCREEN", body).getStatus(), "ownership is never granted");
    }

    @Test
    public void viewerDocumentsDropDraftsAndUnpublishedScreens()
    {
        assertTrue(DashboardViewerDocument.of("{\"version\":3,\"draft\":{}}", null).isEmpty());
        assertTrue(DashboardViewerDocument.of(null, null).isEmpty());
        String whole = DashboardViewerDocument.of(SCREEN, null).orElseThrow();
        assertFalse(whole.contains("secret"));
        assertEquals(Set.of("SALES", "COSTS"), DashboardViewerDocument.reportCodes(whole));
        assertEquals(Set.of("row", "chart", "table"), DashboardViewerDocument.publishedWidgetIds(SCREEN));
        String legacy = DashboardViewerDocument.of("[{\"original\":{\"code\":\"A\"}},{\"original\":{\"code\":\"B\"}}]", Set.of("legacy-1")).orElseThrow();
        assertEquals(Set.of("B"), DashboardViewerDocument.reportCodes(legacy));
        assertNull(SharePermission.grantable("OWNER").orElse(null));
        assertTrue(SharePermission.PUBLISH.atLeast(SharePermission.EDIT));
    }

    @Test
    public void sharesAreStoredAndResolvedAcrossMembersRolesAndEveryone() throws Exception
    {
        try (PersistenceContext context = PersistenceContext.start("h2")) {
            DashboardShareRepository repository = context.bean(DashboardShareRepository.class);
            DashboardShareRepository.Share share = new DashboardShareRepository.Share();
            share.setDashboardId(9L);
            share.setEveryonePermission("VIEW");
            share.setPublicEnabled(true);
            share.setPublicToken("token-abc");
            share.setWidgets("[\"chart\"]");
            repository.save(share);
            repository.replaceMembers(9L, List.of(new DashboardShareRepository.Member("ROLE", 7L, "EDIT"),
                    new DashboardShareRepository.Member("USER", 1L, "PUBLISH")));
            DashboardShareRepository.Share other = new DashboardShareRepository.Share();
            other.setDashboardId(10L);
            repository.save(other);
            repository.replaceMembers(10L, List.of(new DashboardShareRepository.Member("ROLE", 7L, "VIEW")));

            assertEquals("[\"chart\"]", repository.find(9L).orElseThrow().getWidgets());
            assertEquals(Optional.of(9L), repository.findPublic("token-abc"));
            Map<Long, SharePermission> mine = repository.sharedWith(1L, List.of(7L));
            assertEquals(SharePermission.PUBLISH, mine.get(9L), "the highest grant wins");
            assertEquals(SharePermission.VIEW, mine.get(10L));
            assertEquals(Map.of(9L, SharePermission.VIEW), repository.sharedWith(3L, List.of()), "everyone's grant only");

            share.setPublicEnabled(false);
            repository.save(share);
            assertTrue(repository.findPublic("token-abc").isEmpty());
            repository.delete(9L);
            assertTrue(repository.find(9L).isEmpty());
            assertTrue(repository.members(9L).isEmpty());
        }
    }

    @Test
    public void sharedScreensListTheUsersPermissionAndHideOwnScreens()
    {
        DashboardEntity own = DashboardEntity.builder().id(11L).code("OWN").userId(1L).configure(SCREEN).build();
        when(shares.sharedWith(anyLong(), any())).thenReturn(Map.of(9L, SharePermission.VIEW, 11L, SharePermission.EDIT));
        when(dashboards.findAllById(any())).thenReturn(List.of(screen, own));
        grantRole("VIEW", null);
        List<DashboardEntity> listed = service.shared().getData();
        assertEquals(1, listed.size());
        assertEquals("VIEW", listed.get(0).getPermission());
        assertEquals("owner", listed.get(0).getOwnerName());
        assertFalse(listed.get(0).getConfigure().contains("secret"), "viewers list without the draft");
    }
}
