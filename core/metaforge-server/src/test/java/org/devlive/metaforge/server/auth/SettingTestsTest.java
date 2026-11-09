package org.devlive.metaforge.server.auth;

import org.devlive.metaforge.fs.FsRequest;
import org.devlive.metaforge.fs.FsResponse;
import org.devlive.metaforge.fs.FsService;
import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.plugin.PluginMetadata;
import org.devlive.metaforge.plugin.PluginType;
import org.devlive.metaforge.plugin.configure.RuntimeConfigureField;
import org.devlive.metaforge.plugin.configure.RuntimeConfigureFieldType;
import org.devlive.metaforge.service.initializer.InitializerConfigure;
import org.devlive.metaforge.service.service.RuntimeConfigureService;
import org.devlive.metaforge.service.service.impl.SystemSettingServiceImpl;
import org.devlive.metaforge.service.settings.FsSettings;
import org.devlive.metaforge.service.settings.MailService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** The test buttons of System settings try the values in the form before anything is saved. */
public class SettingTestsTest
{
    /** An executor whose batch size must be a number. */
    public static class ProbeExecutor
            extends Plugin
    {
        @Override
        public PluginType getType()
        {
            return PluginType.EXECUTOR;
        }

        @Override
        public List<RuntimeConfigureField> runtimeConfigures()
        {
            return List.of(new RuntimeConfigureField("batchSize", RuntimeConfigureFieldType.NUMBER, "1000", "Rows per batch"));
        }
    }

    private static SystemSettingServiceImpl service(PluginManager plugins)
    {
        RuntimeConfigureService configures = mock(RuntimeConfigureService.class);
        when(configures.getEffective(anyString(), anyString(), any())).thenReturn(Map.of());
        InitializerConfigure initializer = mock(InitializerConfigure.class);
        when(initializer.getDataHome()).thenReturn("/tmp/metaforge-data");
        return new SystemSettingServiceImpl(configures, initializer, mock(MailService.class), plugins);
    }

    @Test
    public void executorsCheckTheirOwnValues()
    {
        PluginManager plugins = mock(PluginManager.class);
        when(plugins.getPluginInfos()).thenReturn(List.of(PluginMetadata.builder().name("Probe").type(PluginType.EXECUTOR).instance(new ProbeExecutor()).build()));
        SystemSettingServiceImpl settings = service(plugins);

        assertTrue(settings.testRuntime("EXECUTOR", "Probe", Map.of("batchSize", "500")).getStatus());
        assertFalse(settings.testRuntime("EXECUTOR", "Probe", Map.of("batchSize", "many")).getStatus());
        assertFalse(settings.testRuntime("EXECUTOR", "Missing", Map.of()).getStatus());
        assertFalse(settings.testRuntime("UNKNOWN", "Probe", Map.of()).getStatus());
    }

    @Test
    public void storageTestsWriteAndRemoveAProbeFile()
    {
        List<String> calls = new ArrayList<>();
        FsService fs = new FsService()
        {
            @Override
            public FsResponse writer(FsRequest request)
            {
                calls.add("write " + request.getEndpoint() + " " + request.getFileName());
                FsResponse response = new FsResponse();
                response.setSuccessful(!"broken".equals(request.getBucket()));
                response.setMessage("bucket refused");
                return response;
            }

            @Override
            public FsResponse reader(FsRequest request)
            {
                return new FsResponse();
            }

            @Override
            public FsResponse delete(FsRequest request)
            {
                calls.add("delete " + request.getFileName());
                return new FsResponse();
            }
        };
        Plugin plugin = mock(Plugin.class);
        when(plugin.getService(FsService.class)).thenReturn(fs);
        PluginManager plugins = mock(PluginManager.class);
        when(plugins.getPlugin("LocalFs")).thenReturn(Optional.of(plugin));
        when(plugins.getPlugin("AmazonS3Fs")).thenReturn(Optional.of(plugin));
        SystemSettingServiceImpl settings = service(plugins);

        assertTrue(settings.testFs(FsSettings.builder().type("LocalFs").build()).getStatus());
        assertEquals(2, calls.size());
        assertTrue(calls.get(0).startsWith("write /tmp/metaforge-data/test metaforge-test/"));
        assertTrue(calls.get(1).startsWith("delete metaforge-test/"));

        FsSettings s3 = FsSettings.builder().type("AmazonS3Fs").access("key").secret("secret").endpoint("s3.example.com").bucket("broken").build();
        assertEquals("Writing a test file failed: bucket refused", settings.testFs(s3).getMessage());
        // An object store missing its bucket is refused before anything is written.
        calls.clear();
        assertFalse(settings.testFs(s3.toBuilder().bucket("").build()).getStatus());
        assertTrue(calls.isEmpty());
    }
}
