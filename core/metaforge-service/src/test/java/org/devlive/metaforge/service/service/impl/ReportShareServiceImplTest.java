package org.devlive.metaforge.service.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.entity.DataSetEntity;
import org.devlive.metaforge.service.entity.ReportEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.enums.ReportType;
import org.devlive.metaforge.service.repository.DataSetRepository;
import org.devlive.metaforge.service.repository.ReportRepository;
import org.devlive.metaforge.service.repository.SourceRepository;
import org.devlive.metaforge.service.repository.UserRepository;
import org.devlive.metaforge.service.repository.jdbc.LinkRepository;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.devlive.metaforge.service.service.DataSetService;
import org.devlive.metaforge.service.service.ReportService;
import org.devlive.metaforge.service.service.ReportShareService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ReportShareServiceImplTest
{
    private static final long OWNER = 7L;
    private static final String CHART = "{\"queryChart\":{\"version\":1},\"columns\":[]}";

    private final List<ReportEntity> reports = new ArrayList<>();
    private final AtomicInteger adhocCalls = new AtomicInteger();
    private final AtomicReference<Long> adhocUser = new AtomicReference<>();
    private ReportShareServiceImpl service;

    @SuppressWarnings("unchecked")
    private static <T> T fake(Class<T> type, java.util.function.BiFunction<String, Object[], Object> handler)
    {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return "equals".equals(method.getName()) ? proxy == args[0] : "hashCode".equals(method.getName()) ? System.identityHashCode(proxy) : type.getSimpleName();
            }
            if (method.isDefault()) {
                return java.lang.invoke.MethodHandles.privateLookupIn(type, java.lang.invoke.MethodHandles.lookup())
                        .unreflectSpecial(method, type).bindTo(proxy).invokeWithArguments(args == null ? new Object[0] : args);
            }
            return handler.apply(method.getName(), args);
        });
    }

    private ReportRepository reportRepository()
    {
        return fake(ReportRepository.class, (name, args) -> {
            switch (name) {
                case "findByCode":
                    return reports.stream().filter(r -> Objects.equals(r.getCode(), args[0])).findFirst();
                case "findByShareToken":
                    return reports.stream().filter(r -> Objects.equals(r.getShareToken(), args[0])).findFirst();
                case "findById":
                    return reports.stream().filter(r -> Objects.equals(r.getId(), args[0])).findFirst();
                case "save":
                    return args[0];
                default:
                    throw new UnsupportedOperationException(name);
            }
        });
    }

    @Before
    public void initialize()
    {
        UserEntity owner = new UserEntity();
        owner.setId(OWNER);
        owner.setUsername("owner");
        UserRepository users = fake(UserRepository.class, (name, args) -> Objects.equals(args[0], OWNER) ? Optional.of(owner) : Optional.empty());
        DataSetEntity dataset = new DataSetEntity();
        dataset.setId(3L);
        dataset.setCode("ds");
        DataSetRepository datasets = fake(DataSetRepository.class, (name, args) -> Optional.of(dataset));
        DataSetService dataSetService = fake(DataSetService.class, (name, args) -> {
            adhocCalls.incrementAndGet();
            adhocUser.set(UserDetailsService.getUser().getId());
            return CommonResponse.success(Map.of("columns", List.of()));
        });
        ReportService reportService = fake(ReportService.class, (name, args) -> CommonResponse.success(List.of()));
        service = new ReportShareServiceImpl(reportRepository(), users, datasets, reportService, dataSetService, new ObjectMapper());
        signIn(OWNER);
    }

    @After
    public void close()
    {
        SecurityContextHolder.clearContext();
    }

    private static void signIn(Long id)
    {
        UserDetailsService principal = new UserDetailsService(id, "u" + id, "u" + id, "", new ArrayList<>(), null);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, "", principal.getAuthorities()));
    }

    private ReportEntity report(String code, ReportType type, String configure)
    {
        ReportEntity report = new ReportEntity();
        report.setId((long) reports.size() + 1);
        report.setCode(code);
        report.setName(code);
        report.setUserId(OWNER);
        report.setType(type);
        report.setConfigure(configure);
        report.setDatasetId(3L);
        report.setQuery("{\"columns\":[],\"limit\":10}");
        reports.add(report);
        return report;
    }

    private static ReportShareService.ShareRequest request(boolean enabled, boolean embed, String origins, boolean rotate)
    {
        return new ReportShareService.ShareRequest(enabled, embed, origins, rotate);
    }

    @Test
    public void onlyTheOwnerSharesAndTheLinkCanBeRotated()
    {
        report("chart", ReportType.QUERY, CHART);
        signIn(99L);
        assertFalse(service.share("chart", request(true, false, "", false)).getStatus());
        signIn(OWNER);
        CommonResponse<ReportShareService.ShareState> shared = service.share("chart", request(true, false, "", false));
        assertTrue(shared.getStatus());
        String token = shared.getData().getToken();
        assertTrue(ReportShareServiceImpl.TOKEN.matcher(token).matches());
        assertEquals(token, service.share("chart", request(true, false, "", false)).getData().getToken());
        String rotated = service.share("chart", request(true, false, "", true)).getData().getToken();
        assertNotEquals(token, rotated);
        assertFalse("the old link stops working", service.view(token).getStatus());
        assertTrue(service.view(rotated).getStatus());
    }

    @Test
    public void earlierQueryReportsCannotBeShared()
    {
        report("legacy", ReportType.QUERY, "{\"type\":\"LINE\"}");
        assertFalse(service.share("legacy", request(true, false, "", false)).getStatus());
    }

    @Test
    public void thePublicViewHidesSqlSourceAndOwner()
    {
        ReportEntity report = report("chart", ReportType.QUERY, CHART);
        report.setDescription("Weekly");
        String token = service.share("chart", request(true, false, "", false)).getData().getToken();
        ReportShareService.PublicReport view = service.view(token).getData();
        assertEquals("chart", view.getName());
        assertEquals(CHART, view.getConfigure());
        assertEquals("QUERY", view.getType());
        assertFalse(view.isEmbed());
        service.share("chart", request(false, false, "", false));
        assertFalse("disabled links are gone", service.view(token).getStatus());
        assertFalse(service.view("short").getStatus());
        assertFalse(service.view(null).getStatus());
    }

    @Test
    public void framingFollowsTheOwnersChoice()
    {
        report("chart", ReportType.QUERY, CHART);
        String token = service.share("chart", request(true, false, "", false)).getData().getToken();
        assertFalse(service.frameAncestors(token).isPresent());
        service.share("chart", request(true, true, "", false));
        assertEquals("*", service.frameAncestors(token).orElse(null));
        service.share("chart", request(true, true, "https://a.example.com/\nhttps://a.example.com, https://*.b.org:8443", false));
        assertEquals("'self' https://a.example.com https://*.b.org:8443", service.frameAncestors(token).orElse(null));
        assertFalse(service.share("chart", request(true, true, "javascript:alert(1)", false)).getStatus());
        assertFalse(service.share("chart", request(true, true, "https://a.com/path", false)).getStatus());
    }

    @Test
    public void datasetDataRunsAsTheOwnerAndIsCached()
    {
        report("ds-report", ReportType.DATASET, "{}");
        String token = service.share("ds-report", request(true, false, "", false)).getData().getToken();
        SecurityContextHolder.clearContext();
        assertTrue(service.data(token).getStatus());
        assertEquals(Long.valueOf(OWNER), adhocUser.get());
        assertNull("the visitor stays anonymous afterwards", SecurityContextHolder.getContext().getAuthentication());
        assertTrue(service.data(token).getStatus());
        assertEquals("public views share one cached result", 1, adhocCalls.get());
        assertFalse("a dataset report has no chart refresh", service.refresh(token).getStatus());
    }

    @Test
    public void savingAReportNeverChangesItsSharing()
    {
        SourceRepository sources = fake(SourceRepository.class, (name, args) -> Optional.empty());
        DataSetEntity dataset = new DataSetEntity();
        dataset.setCode("ds");
        DataSetRepository datasets = fake(DataSetRepository.class, (name, args) -> Optional.of(dataset));
        LinkRepository links = null;
        ReportServiceImpl reportService = new ReportServiceImpl(reportRepository(), sources, datasets, links, new QueryChartRefresh(new ObjectMapper(), null));

        ReportEntity created = new ReportEntity();
        created.setDataset(dataset);
        created.setShareEnabled(true);
        created.setShareToken("chosen-by-the-client-000000000000");
        ReportEntity saved = reportService.saveOrUpdate(fake(ReportRepository.class, (name, args) -> args[0]), created).getData();
        assertFalse(saved.isShareEnabled());
        assertNull(saved.getShareToken());

        ReportEntity stored = report("ds-report", ReportType.DATASET, "{}");
        String token = service.share("ds-report", request(true, true, "https://a.com", false)).getData().getToken();
        ReportEntity update = new ReportEntity();
        update.setId(stored.getId());
        update.setDataset(dataset);
        ReportEntity kept = reportService.saveOrUpdate(fake(ReportRepository.class, (name, args) -> args[0]), update).getData();
        assertNotNull(kept);
        assertTrue(kept.isShareEnabled());
        assertTrue(kept.isShareEmbed());
        assertEquals(token, kept.getShareToken());
        assertEquals("https://a.com", kept.getShareOrigins());
        assertTrue(kept.isShareShowTitle());
    }

    @Test
    public void internalErrorsNeverReachAnonymousVisitors()
    {
        ReportRepository broken = fake(ReportRepository.class, (name, args) -> {
            throw new IllegalStateException("bad SQL grammar [SELECT secret FROM metaforge_report]");
        });
        ReportShareServiceImpl failing = new ReportShareServiceImpl(broken, null, null, null, null, new ObjectMapper());
        String token = "abcdefghijklmnopqrstuvwxyz012-_A";
        CommonResponse<ReportShareService.PublicReport> view = failing.view(token);
        assertFalse(view.getStatus());
        assertFalse(String.valueOf(view.getMessage()).contains("SQL"));
        assertFalse(failing.frameAncestors(token).isPresent());
        assertFalse(failing.data(token).getStatus());
    }

    @Test
    public void linksExpireAndTheOwnerChoosesWhatThePageShows()
    {
        ReportEntity report = report("chart", ReportType.QUERY, CHART);
        report.setDescription("Weekly");
        long past = System.currentTimeMillis() - 1000;
        assertFalse("an expiry in the past is refused",
                service.share("chart", new ReportShareService.ShareRequest(true, true, "", false, past, null, null)).getStatus());
        long future = System.currentTimeMillis() + 3_600_000;
        CommonResponse<ReportShareService.ShareState> shared = service.share("chart", new ReportShareService.ShareRequest(true, true, "", false, future, false, false));
        assertTrue(shared.getStatus());
        assertEquals(Long.valueOf(future), shared.getData().getExpiresAt());
        String token = shared.getData().getToken();
        ReportShareService.PublicReport view = service.view(token).getData();
        assertFalse(view.isShowTitle());
        assertFalse(view.isShowDescription());
        assertNull("a hidden description is not sent", view.getDescription());

        report.setShareExpiresAt(new java.util.Date(past));
        assertFalse("expired links stop working", service.view(token).getStatus());
        assertFalse(service.frameAncestors(token).isPresent());
        assertTrue("an owner can extend the link", service.share("chart", new ReportShareService.ShareRequest(true, true, "", false, null, null, null)).getStatus());
        ReportShareService.PublicReport again = service.view(token).getData();
        assertTrue("omitted display options keep the page complete", again.isShowTitle() && again.isShowDescription());
        assertEquals("Weekly", again.getDescription());
    }
}
