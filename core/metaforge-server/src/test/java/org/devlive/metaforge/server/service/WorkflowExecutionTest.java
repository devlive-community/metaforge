package org.devlive.metaforge.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.executor.ExecutorService;
import org.devlive.metaforge.executor.common.RunState;
import org.devlive.metaforge.executor.configure.ExecutorMetrics;
import org.devlive.metaforge.executor.configure.ExecutorRequest;
import org.devlive.metaforge.executor.configure.ExecutorResponse;
import org.devlive.metaforge.executor.workflow.WorkflowPlan;
import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.plugin.PluginMetadata;
import org.devlive.metaforge.plugin.PluginType;
import org.devlive.metaforge.server.support.PersistenceContext;
import org.devlive.metaforge.service.body.FilterBody;
import org.devlive.metaforge.service.body.FilterCriterionBody;
import org.devlive.metaforge.service.entity.SourceEntity;
import org.devlive.metaforge.service.entity.WorkflowEntity;
import org.devlive.metaforge.service.initializer.InitializerConfigure;
import org.devlive.metaforge.service.itransient.configuration.Configuration;
import org.devlive.metaforge.service.itransient.configuration.ConnectionConfiguration;
import org.devlive.metaforge.service.itransient.configuration.NodeConfiguration;
import org.devlive.metaforge.service.repository.SourceRepository;
import org.devlive.metaforge.service.repository.WorkflowRepository;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.devlive.metaforge.service.service.RuntimeConfigureService;
import org.devlive.metaforge.service.service.impl.WorkflowServiceImpl;
import org.devlive.metaforge.service.workflow.WorkflowCatalog;
import org.devlive.metaforge.service.workflow.WorkflowRun;
import org.devlive.metaforge.spi.PluginService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WorkflowExecutionTest
{
    @TempDir
    Path home;
    private final WorkflowRepository repository = mock(WorkflowRepository.class);
    private final SourceRepository sources = mock(SourceRepository.class);
    private final PluginManager plugins = mock(PluginManager.class);
    private final RuntimeConfigureService runtime = mock(RuntimeConfigureService.class);
    private final ExecutorService executor = mock(ExecutorService.class);
    private final ExecutorService seatunnel = mock(ExecutorService.class);
    private WorkflowServiceImpl service;
    private WorkflowCatalog catalog;

    @BeforeEach
    public void setup() throws Exception
    {
        UserDetailsService user = new UserDetailsService(1L, "USER", "owner", "", List.of(), null);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, "", user.getAuthorities()));
        InitializerConfigure initializer = mock(InitializerConfigure.class);
        when(initializer.getDataHome()).thenReturn(home.toString());
        engine("MetaForge", executor, "metaforge-executor-metaforge");
        engine("Seatunnel", seatunnel, "metaforge-executor-seatunnel");
        when(plugins.getPluginInfos()).thenReturn(List.of(
                PluginMetadata.builder().name("Seatunnel").type(PluginType.EXECUTOR).build(),
                PluginMetadata.builder().name("MetaForge").type(PluginType.EXECUTOR).build(),
                PluginMetadata.builder().name("MockJdbc").type(PluginType.CONNECTOR).build()));
        when(runtime.getEffective(anyString(), anyString(), any())).thenReturn(Map.of("batchSize", "1000"));
        // Compilation belongs to each executor and is tested with it; here it only has to reach the executor.
        when(executor.compile(any())).thenAnswer(call -> request(call.getArgument(0)));
        when(seatunnel.compile(any())).thenAnswer(call -> request(call.getArgument(0)));
        catalog = new WorkflowCatalog(plugins);
        service = new WorkflowServiceImpl(initializer, plugins, repository, sources, runtime, catalog);
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    /** Registers an executor plugin serving the declaration that ships in the executor module. */
    private void engine(String name, ExecutorService service, String module) throws Exception
    {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getName()).thenReturn(name);
        when(plugin.getType()).thenReturn(PluginType.EXECUTOR);
        when(plugin.getService(ExecutorService.class)).thenReturn(service);
        when(plugin.runtimeConfigures()).thenReturn(List.of());
        when(plugins.getPlugin(name)).thenReturn(Optional.of(plugin));
        Path declaration = Path.of("../../executor", module, "src/main/resources/org/devlive/metaforge/executor/workflow/workflow.yaml");
        when(service.workflowDeclaration()).thenReturn(declaration.toUri().toURL());
    }

    private static ExecutorRequest request(WorkflowPlan plan)
    {
        ExecutorRequest request = new ExecutorRequest(plan.getTaskName(), plan.getUserName(),
                plan.source().getSources().isEmpty() ? new org.devlive.metaforge.executor.configure.ExecutorConfigure("Jdbc") : plan.source().source("source"),
                plan.sink().getSources().isEmpty() ? new org.devlive.metaforge.executor.configure.ExecutorConfigure("Jdbc") : plan.sink().source("source"));
        request.setWorkHome(plan.getWorkHome());
        request.setOptions(plan.getOptions());
        return request;
    }

    @AfterEach
    public void close()
    {
        service.close();
        SecurityContextHolder.clearContext();
    }

    private WorkflowEntity workflow()
    {
        NodeConfiguration input = new NodeConfiguration();
        input.setId(UUID.randomUUID().toString());
        input.setTid("metaforge-source");
        input.setKey("Source");
        input.setCategory("source");
        input.setData(Map.of("source", "SOURCE", "query", "select id from events"));
        NodeConfiguration output = new NodeConfiguration();
        output.setId(UUID.randomUUID().toString());
        output.setTid("metaforge-sink");
        output.setKey("Sink");
        output.setCategory("sink");
        output.setData(Map.of("source", "TARGET", "database", "warehouse", "table", "events", "columns", Map.of("id", "event_id")));
        Configuration graph = new Configuration();
        graph.setNodes(List.of(input, output));
        graph.setConnections(List.of(new ConnectionConfiguration("connection", input.getId() + "-output", output.getId() + "-input")));
        WorkflowEntity workflow = new WorkflowEntity();
        workflow.setCode("WORKFLOW");
        workflow.setName("Workflow");
        workflow.setExecutor("MetaForge");
        workflow.setUserId(1L);
        workflow.setConfigure(graph);
        return workflow;
    }

    @Test
    public void catalogAndGraphValidationFollowTheExecutor()
    {
        assertEquals(List.of("MetaForge", "Seatunnel"), catalog.executors().stream().map(item -> item.definition().getName()).collect(Collectors.toList()));
        assertEquals(List.of("metaforge-sink", "metaforge-source"), catalog.definitions("MetaForge").stream().map(NodeConfiguration::getId).sorted().collect(Collectors.toList()));
        assertTrue(catalog.definitions("Seatunnel").stream().noneMatch(node -> node.getId().startsWith("metaforge")));
        assertTrue(catalog.executor("MetaForge").definition().getCapabilities().cooperativeStop());
        assertFalse(catalog.executor("Seatunnel").definition().getCapabilities().isRowCount());
        assertEquals(0, catalog.executor("MetaForge").definition().limit("transform"));
        assertEquals(3, catalog.executor("Seatunnel").definition().limit("transform"));
        WorkflowEntity workflow = workflow();
        assertTrue(catalog.validate(workflow).isEmpty());
        workflow.getConfigure().getConnections().get(0).setTarget(workflow.getConfigure().getNodes().get(0).getId() + "-input");
        assertFalse(catalog.validate(workflow).isEmpty());
        workflow = workflow();
        workflow.getConfigure().getNodes().get(0).setTid("sql-transform");
        assertFalse(catalog.validate(workflow).isEmpty());
        workflow = workflow();
        workflow.getConfigure().setSettings(Map.of("batchSize", 0));
        assertTrue(catalog.validate(workflow).stream().anyMatch(error -> error.contains("out of range")));
        workflow.getConfigure().setSettings(Map.of("batchSize", 200));
        assertTrue(catalog.validate(workflow).isEmpty());
    }

    @Test
    public void executorSpecificValidationRunsAfterTheGenericChecks()
    {
        WorkflowEntity workflow = workflow();
        when(executor.validate(any())).thenReturn(List.of("Executor rejected the pipeline"));
        assertEquals(List.of("Executor rejected the pipeline"), catalog.validate(workflow));
        WorkflowEntity broken = workflow();
        broken.getConfigure().setConnections(List.of());
        assertFalse(catalog.validate(broken).contains("Executor rejected the pipeline"));
    }

    @Test
    public void savingADraftDoesNotRunItAndPublishingOnlyValidates()
    {
        WorkflowEntity workflow = workflow();
        when(repository.findByCode("WORKFLOW")).thenReturn(Optional.of(workflow));
        assertTrue(service.saveOrUpdate(repository, workflow).getStatus());
        assertEquals(RunState.CREATED, workflow.getState());
        assertTrue(service.publish("WORKFLOW").getStatus());
        assertNotNull(workflow.getConfigure().getPublishedAt());
        assertEquals(1, workflow.getConfigure().getRevision());
        verify(executor, never()).start(any());
    }

    @Test
    public void foreignWorkflowsCannotBeReadChangedExecutedOrDeleted()
    {
        WorkflowEntity foreign = workflow();
        foreign.setUserId(2L);
        when(repository.findByCode("WORKFLOW")).thenReturn(Optional.of(foreign));
        assertFalse(service.getByCode(repository, "WORKFLOW").getStatus());
        assertFalse(service.saveOrUpdate(repository, workflow()).getStatus());
        assertFalse(service.restart("WORKFLOW").getStatus());
        assertFalse(service.publish("WORKFLOW").getStatus());
        assertFalse(service.runs("WORKFLOW").getStatus());
        assertFalse(service.log("WORKFLOW").getStatus());
        assertFalse(service.stop("WORKFLOW").getStatus());
        assertFalse(service.deleteByCode(repository, "WORKFLOW").getStatus());
        verify(repository, never()).save(any());
        verify(repository, never()).delete(any());
    }

    @Test
    public void executionUsesSavedSourcesAndPersistsRealRunResults() throws Exception
    {
        WorkflowEntity workflow = workflow();
        when(repository.findByCode("WORKFLOW")).thenReturn(Optional.of(workflow));
        Plugin connector = mock(Plugin.class);
        PluginService driver = mock(PluginService.class);
        when(connector.getService(PluginService.class)).thenReturn(driver);
        when(driver.supportsStreaming()).thenReturn(true);
        when(plugins.getPlugin("MockJdbc")).thenReturn(Optional.of(connector));
        for (String code : List.of("SOURCE", "TARGET")) {
            SourceEntity source = new SourceEntity();
            source.setCode(code); source.setUserId(1L); source.setType("MockJdbc");
            when(sources.findByCode(code)).thenReturn(Optional.of(source));
        }
        AtomicReference<ExecutorRequest> submitted = new AtomicReference<>();
        CountDownLatch completed = new CountDownLatch(1);
        when(executor.start(any())).thenAnswer(call -> {
            submitted.set(call.getArgument(0));
            ExecutorResponse response = new ExecutorResponse(false, true, RunState.SUCCESS, null);
            response.setCount(7);
            ExecutorMetrics metrics = new ExecutorMetrics();
            metrics.setRowsWritten(7);
            response.setMetrics(metrics);
            response.setPreview(List.of(Map.of("event_id", 3)));
            completed.countDown();
            return response;
        });
        CommonResponse<String> started = service.restart("WORKFLOW");
        assertTrue(started.getStatus(), String.valueOf(started.getMessage()));
        assertTrue(completed.await(3, TimeUnit.SECONDS));
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        WorkflowRun run;
        do {
            run = service.run("WORKFLOW", started.getData()).getData();
            if (run.getState() != RunState.SUCCESS) { Thread.sleep(5); }
        } while (run.getState() != RunState.SUCCESS && System.nanoTime() < deadline);
        assertEquals(RunState.SUCCESS, run.getState());
        assertEquals(7, run.getCount());
        assertEquals(3, run.getPreview().get(0).get("event_id"));
        verify(executor).compile(org.mockito.ArgumentMatchers.argThat(plan -> plan.getTaskName().equals(started.getData())
                && plan.getNodes().size() == 2 && "source".equals(plan.getNodes().get(0).getCategory().getValue())
                && plan.source().getSources().containsKey("source") && plan.sink().getSources().containsKey("source")
                && "warehouse".equals(plan.sink().getValues().get("database"))
                && "1000".equals(plan.getOptions().get("batchSize"))));
        assertEquals(started.getData(), submitted.get().getTaskName());
        assertNull(service.runs("WORKFLOW").getData().get(0).getPreview());
        Path persisted = Path.of(workflow.getWork(), "run-" + started.getData() + ".json");
        WorkflowRun disk;
        do {
            disk = new ObjectMapper().readValue(persisted.toFile(), WorkflowRun.class);
            if (disk.getState() != RunState.SUCCESS) { Thread.sleep(5); }
        } while (disk.getState() != RunState.SUCCESS && System.nanoTime() < deadline);
        assertEquals(RunState.SUCCESS, disk.getState());
        assertEquals(7, disk.getMetrics().getRowsWritten());
        assertEquals(3, disk.getPreview().get(0).get("event_id"));
        assertFalse(service.log("WORKFLOW", "../../secrets").getStatus());
        verify(sources).findByCode(eq("SOURCE"));
    }
    @Test
    public void stoppingIsCooperativeAndPreventsAnOverlappingRestart() throws Exception
    {
        WorkflowEntity workflow = workflow();
        when(repository.findByCode("WORKFLOW")).thenReturn(Optional.of(workflow));
        Plugin connector = mock(Plugin.class);
        PluginService driver = mock(PluginService.class);
        when(connector.getService(PluginService.class)).thenReturn(driver);
        when(driver.supportsStreaming()).thenReturn(true);
        when(plugins.getPlugin("MockJdbc")).thenReturn(Optional.of(connector));
        for (String code : List.of("SOURCE", "TARGET")) {
            SourceEntity source = new SourceEntity();
            source.setCode(code); source.setUserId(1L); source.setType("MockJdbc");
            when(sources.findByCode(code)).thenReturn(Optional.of(source));
        }
        CountDownLatch running = new CountDownLatch(1);
        CountDownLatch exiting = new CountDownLatch(1);
        when(executor.start(any())).thenAnswer(call -> {
            running.countDown();
            assertTrue(exiting.await(3, TimeUnit.SECONDS));
            return new ExecutorResponse(false, true, RunState.SUCCESS, null);
        });
        when(executor.stop(any())).thenReturn(new ExecutorResponse(false, true, RunState.STOPPED, null));
        CommonResponse<String> started = service.restart("WORKFLOW");
        assertTrue(started.getStatus());
        try {
            assertTrue(running.await(3, TimeUnit.SECONDS));
            assertTrue(service.stop("WORKFLOW").getStatus());
            assertEquals(RunState.STOPPED, workflow.getState());
            assertFalse(service.restart("WORKFLOW").getStatus());
            assertEquals(RunState.STOPPED, service.run("WORKFLOW", started.getData()).getData().getState());
        }
        finally {
            exiting.countDown();
        }
    }

    @Test
    public void missingExternalEngineCountsRemainUnknown() throws Exception
    {
        WorkflowEntity workflow = workflow();
        workflow.setExecutor("Seatunnel");
        for (NodeConfiguration node : workflow.getConfigure().getNodes()) {
            node.setTid("source".equals(node.getCategory()) ? "jdbc-source" : "jdbc-sink");
            node.setKey("Jdbc");
            node.setData(Map.of("url", "jdbc:example:test", "driver", "com.mysql.jdbc.Driver"));
        }
        when(repository.findByCode("WORKFLOW")).thenReturn(Optional.of(workflow));
        when(seatunnel.start(any())).thenReturn(new ExecutorResponse(false, true, RunState.SUCCESS, null));
        CommonResponse<String> started = service.restart("WORKFLOW");
        assertTrue(started.getStatus(), String.valueOf(started.getMessage()));
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        WorkflowRun run;
        do {
            run = service.run("WORKFLOW", started.getData()).getData();
            if (run.getState() != RunState.SUCCESS) { Thread.sleep(5); }
        } while (run.getState() != RunState.SUCCESS && System.nanoTime() < deadline);
        assertEquals(RunState.SUCCESS, run.getState());
        assertNull(run.getCount());
        verify(executor, never()).start(any());
    }

    @Test
    public void savedWorkflowCanBeReopenedFromJdbc() throws Exception
    {
        try (PersistenceContext context = PersistenceContext.start("h2")) {
            WorkflowRepository realRepository = context.bean(WorkflowRepository.class);
            InitializerConfigure initializer = mock(InitializerConfigure.class);
            when(initializer.getDataHome()).thenReturn(home.toString());
            WorkflowServiceImpl realService = new WorkflowServiceImpl(initializer, plugins, realRepository, sources, runtime, catalog);
            try {
                WorkflowEntity draft = workflow();
                draft.getConfigure().setDescription("Persisted workflow description");
                draft.getConfigure().setTimeout(120L);
                CommonResponse<WorkflowEntity> saved = realService.saveOrUpdate(realRepository, draft);
                assertTrue(saved.getStatus(), String.valueOf(saved.getMessage()));
                assertTrue(realService.publish("WORKFLOW").getStatus());
                WorkflowEntity reopened = realService.getByCode(realRepository, "WORKFLOW").getData();
                assertEquals(RunState.CREATED, reopened.getState());
                assertEquals("Persisted workflow description", reopened.getConfigure().getDescription());
                assertEquals(120L, reopened.getConfigure().getTimeout());
                assertEquals(1, reopened.getConfigure().getRevision());
                assertNotNull(reopened.getConfigure().getPublishedAt());
                assertEquals("event_id", ((Map<?, ?>) reopened.getConfigure().getNodes().get(1).getData().get("columns")).get("id"));
                assertEquals(1L, realService.getAll(realRepository, new FilterBody()).getData().getTotal());
                FilterBody filter = new FilterBody();
                filter.setConditions(Map.of("state", new FilterCriterionBody("FAILURE", true)));
                assertEquals(0L, realService.getAll(realRepository, filter).getData().getTotal());
                filter.setConditions(Map.of("executor", new FilterCriterionBody("MetaForge", true)));
                assertEquals(1L, realService.getAll(realRepository, filter).getData().getTotal());
                verify(executor, never()).start(any());
            }
            finally {
                realService.close();
            }
        }
    }

}
