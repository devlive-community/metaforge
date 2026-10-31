package org.devlive.metaforge.server.service;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.service.entity.SourceDdlHistoryEntity;
import org.devlive.metaforge.service.entity.SourceEntity;
import org.devlive.metaforge.service.repository.SourceDdlHistoryRepository;
import org.devlive.metaforge.service.repository.SourceRepository;
import org.devlive.metaforge.service.service.impl.MetadataServiceImpl;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.generator.definition.TableDefinition;
import org.devlive.metaforge.spi.model.ManagementCapabilities;
import org.devlive.metaforge.spi.model.ManagementPage;
import org.devlive.metaforge.spi.model.ManagementRequest;
import org.devlive.metaforge.spi.model.Response;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class MetadataCapabilityTest
{
    @Test
    public void profilesComeFromThePluginAndDoNotOpenAConnection()
    {
        SourceRepository repository = mock(SourceRepository.class);
        PluginManager manager = mock(PluginManager.class);
        Plugin plugin = mock(Plugin.class);
        PluginService adapter = mock(PluginService.class);
        SourceEntity source = new SourceEntity();
        source.setType("Redis");
        when(repository.findByCode("source-code")).thenReturn(Optional.of(source));
        when(manager.getPlugin("Redis")).thenReturn(Optional.of(plugin));
        when(plugin.getService(PluginService.class)).thenReturn(adapter);
        when(adapter.managementCapabilities()).thenReturn(ManagementCapabilities.forJdbc("redis"));
        MetadataServiceImpl service = new MetadataServiceImpl(manager, repository, mock(SourceDdlHistoryRepository.class));

        CommonResponse<ManagementCapabilities> result = service.getCapabilities("source-code");
        assertTrue(result.getStatus());
        assertEquals("key-value", result.getData().getKind());
        assertFalse(result.getData().supports("column:modify"));
        assertFalse(service.changeColumn("source-code", "0", "key", TableDefinition.create("0", "key")).getStatus());
        assertFalse(service.createTable("source-code", "0", TableDefinition.create("0", "key")).getStatus());
        assertFalse(service.previewCreateTableBatch("source-code", TableDefinition.create("0", "key")).getStatus());
        assertFalse(service.getColumns("source-code", "0", "key").getStatus());
        verify(adapter, never()).connect(any());
        verify(adapter, never()).changeColumn(any(), any());
        verify(adapter, never()).createTable(any(), any());
        verify(adapter, never()).previewCreateTable(any(), any(), anyString());
        verify(adapter, never()).getColumns(any(), any(), any());
    }

    @Test
    public void nativeReadsUseTheNativeAdapterAndDownloadsRemainCapabilityChecked()
    {
        SourceRepository repository = mock(SourceRepository.class);
        PluginManager manager = mock(PluginManager.class);
        Plugin plugin = mock(Plugin.class);
        PluginService adapter = mock(PluginService.class);
        SourceEntity source = new SourceEntity();
        source.setType("Redis");
        when(repository.findByCode("source-code")).thenReturn(Optional.of(source));
        when(manager.getPlugin("Redis")).thenReturn(Optional.of(plugin));
        when(plugin.getService(PluginService.class)).thenReturn(adapter);
        when(adapter.managementCapabilities()).thenReturn(new ManagementCapabilities("key-value", Set.of("key:list", "key:read", "namespace:list")));
        ManagementPage expected = new ManagementPage();
        when(adapter.managementList(any(), any())).thenReturn(expected);
        MetadataServiceImpl service = new MetadataServiceImpl(manager, repository, mock(SourceDdlHistoryRepository.class));
        ManagementRequest request = new ManagementRequest();
        assertTrue(service.managementList("source-code", request).getStatus());
        verify(adapter).managementList(any(), any());
        verify(adapter, never()).getTables(any(), any());
        assertFalse(service.managementDownload("source-code", request).getStatus());
        verify(adapter, never()).managementDownload(any(), any());
    }

    @Test
    public void invalidBatchBudgetsFailBeforeOpeningAPluginOrConnection()
    {
        SourceRepository repository = mock(SourceRepository.class);
        PluginManager manager = mock(PluginManager.class);
        ManagementRequest request = new ManagementRequest();
        request.setLimit(201);
        assertFalse(new MetadataServiceImpl(manager, repository, mock(SourceDdlHistoryRepository.class)).managementList("source-code", request).getStatus());
        verifyNoInteractions(repository, manager);
    }

    @Test
    public void missingSourcesFailBeforeLookingUpPlugins()
    {
        SourceRepository repository = mock(SourceRepository.class);
        PluginManager manager = mock(PluginManager.class);
        when(repository.findByCode("missing")).thenReturn(Optional.empty());
        assertFalse(new MetadataServiceImpl(manager, repository, mock(SourceDdlHistoryRepository.class)).getCapabilities("missing").getStatus());
        verifyNoInteractions(manager);
    }

    @Test
    public void generatedDdlIsRecordedOnlyWhenTheSourceProducedIt()
    {
        SourceRepository repository = mock(SourceRepository.class);
        SourceDdlHistoryRepository history = mock(SourceDdlHistoryRepository.class);
        PluginManager manager = mock(PluginManager.class);
        Plugin plugin = mock(Plugin.class);
        PluginService adapter = mock(PluginService.class);
        SourceEntity source = new SourceEntity();
        source.setType("MySQL");
        when(repository.findByCode("source-code")).thenReturn(Optional.of(source));
        when(manager.getPlugin("MySQL")).thenReturn(Optional.of(plugin));
        when(plugin.getService(PluginService.class)).thenReturn(adapter);
        when(adapter.managementCapabilities()).thenReturn(ManagementCapabilities.forJdbc("mysql"));
        when(adapter.getTableDdl(any(), eq("app"), eq("user"), eq("indexes"))).thenReturn(Response.builder().isSuccessful(true).content("ALTER TABLE x;").build());
        when(adapter.getTableDdl(any(), eq("app"), eq("user"), eq("create"))).thenReturn(Response.builder().isSuccessful(false).message("denied").build());
        MetadataServiceImpl service = new MetadataServiceImpl(manager, repository, history);

        assertTrue(service.generateTableDdl("source-code", "app", "user", "indexes").getStatus());
        verify(history).save(argThat((SourceDdlHistoryEntity entry) -> "indexes".equals(entry.getKind()) && "ALTER TABLE x;".equals(entry.getContent())
                && "app".equals(entry.getDatabaseName()) && "user".equals(entry.getTableName()) && "source-code".equals(entry.getSourceCode())));
        service.generateTableDdl("source-code", "app", "user", "create");
        verify(history, org.mockito.Mockito.times(1)).save(any());
    }

    @Test
    public void onePreviewRequestReturnsAllSectionsWithoutExecutingOrRecordingDdl()
    {
        SourceRepository repository = mock(SourceRepository.class);
        SourceDdlHistoryRepository history = mock(SourceDdlHistoryRepository.class);
        PluginManager manager = mock(PluginManager.class);
        Plugin plugin = mock(Plugin.class);
        PluginService adapter = mock(PluginService.class);
        SourceEntity source = new SourceEntity();
        source.setType("MySQL");
        when(repository.findByCode("source-code")).thenReturn(Optional.of(source));
        when(manager.getPlugin("MySQL")).thenReturn(Optional.of(plugin));
        when(plugin.getService(PluginService.class)).thenReturn(adapter);
        when(adapter.managementCapabilities()).thenReturn(ManagementCapabilities.forJdbc("mysql"));
        TableDefinition definition = TableDefinition.create("app", "users");
        when(adapter.previewCreateTable(any(), eq(definition), anyString()))
                .thenAnswer(invocation -> Response.builder().isSuccessful(true).content(invocation.<String>getArgument(2) + " SQL").build());

        CommonResponse<Map<String, String>> result = new MetadataServiceImpl(manager, repository, history).previewCreateTableBatch("source-code", definition);
        assertTrue(result.getStatus());
        assertEquals(Map.of("full", "full SQL", "columns", "columns SQL", "indexes", "indexes SQL", "options", "options SQL"), result.getData());
        verify(adapter, org.mockito.Mockito.times(4)).previewCreateTable(any(), eq(definition), anyString());
        verify(adapter, never()).connect(any());
        verify(adapter, never()).createTable(any(), any());
        verifyNoInteractions(history);
    }

    @Test
    public void failedPreviewSectionsDoNotProduceAPartialSuccessfulBatch()
    {
        SourceRepository repository = mock(SourceRepository.class);
        PluginManager manager = mock(PluginManager.class);
        Plugin plugin = mock(Plugin.class);
        PluginService adapter = mock(PluginService.class);
        SourceEntity source = new SourceEntity();
        source.setType("MySQL");
        when(repository.findByCode("source-code")).thenReturn(Optional.of(source));
        when(manager.getPlugin("MySQL")).thenReturn(Optional.of(plugin));
        when(plugin.getService(PluginService.class)).thenReturn(adapter);
        when(adapter.managementCapabilities()).thenReturn(ManagementCapabilities.forJdbc("mysql"));
        when(adapter.previewCreateTable(any(), any(), anyString())).thenReturn(Response.builder().isSuccessful(false).message("preview denied").build());
        CommonResponse<Map<String, String>> result = new MetadataServiceImpl(manager, repository, mock(SourceDdlHistoryRepository.class))
                .previewCreateTableBatch("source-code", TableDefinition.create("app", "users"));
        assertFalse(result.getStatus());
        assertEquals("preview denied", result.getMessage());
        verify(adapter, org.mockito.Mockito.times(1)).previewCreateTable(any(), any(), anyString());
    }
}
