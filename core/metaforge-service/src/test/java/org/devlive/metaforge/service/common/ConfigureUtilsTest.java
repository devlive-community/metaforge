package org.devlive.metaforge.service.common;

import org.devlive.metaforge.plugin.configure.PluginConfigure;
import org.devlive.metaforge.plugin.configure.PluginConfigureExecutor;
import org.devlive.metaforge.plugin.configure.PluginConfigureField;
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldName;
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldType;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.devlive.metaforge.plugin.configure.PluginConfigurePipelineType;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigureUtilsTest
{
    @Test
    public void testFindPipelineExecutorSkipsIncompleteEntries()
    {
        // 复现 issue #953：Doris 的 YAML 中存在缺少 executor/type 的 pipeline 条目
        PluginConfigure configure = new PluginConfigure();
        List<PluginConfigureExecutor> pipelines = new ArrayList<>();
        pipelines.add(new PluginConfigureExecutor());
        configure.setPipelines(pipelines);

        Assert.assertNull(ConfigureUtils.findPipelineExecutor(configure, "TRUNCATE", PluginConfigurePipelineType.INPUT));
    }

    @Test
    public void testFindPipelineExecutorMatches()
    {
        PluginConfigure configure = new PluginConfigure();
        PluginConfigureExecutor matched = new PluginConfigureExecutor();
        matched.setExecutor("TRUNCATE");
        matched.setType(PluginConfigurePipelineType.INPUT);
        List<PluginConfigureExecutor> pipelines = new ArrayList<>();
        pipelines.add(new PluginConfigureExecutor());
        pipelines.add(matched);
        configure.setPipelines(pipelines);

        Assert.assertSame(matched, ConfigureUtils.findPipelineExecutor(configure, "TRUNCATE", PluginConfigurePipelineType.INPUT));
    }

    @Test
    public void testFindPipelineExecutorWithNullPipelines()
    {
        PluginConfigure configure = new PluginConfigure();

        Assert.assertNull(ConfigureUtils.findPipelineExecutor(configure, "TRUNCATE", PluginConfigurePipelineType.INPUT));
    }

    @Test
    public void testFindPipelineExecutorWithNullArguments()
    {
        PluginConfigure configure = new PluginConfigure();
        PluginConfigureExecutor matched = new PluginConfigureExecutor();
        matched.setExecutor("TRUNCATE");
        matched.setType(PluginConfigurePipelineType.INPUT);
        List<PluginConfigureExecutor> pipelines = new ArrayList<>();
        pipelines.add(matched);
        configure.setPipelines(pipelines);

        // 请求参数为 null 时也不能抛 NPE
        Assert.assertNull(ConfigureUtils.findPipelineExecutor(configure, null, null));
        Assert.assertSame(matched, ConfigureUtils.findPipelineExecutor(configure, "TRUNCATE", PluginConfigurePipelineType.INPUT));
    }

    @Test
    public void testFieldsSentWithoutValueAreNotApplied()
    {
        // Testing a connection whose custom configures were sent as null must not fail with an NPE.
        List<PluginConfigureField> fields = Arrays.asList(
                PluginConfigureField.builder().field(PluginConfigureFieldName.host).type(PluginConfigureFieldType.String).value("127.0.0.1").build(),
                PluginConfigureField.builder().field(PluginConfigureFieldName.port).type(PluginConfigureFieldType.Number).build(),
                PluginConfigureField.builder().field(PluginConfigureFieldName.configures).type(PluginConfigureFieldType.Array).build());

        List<PluginConfigureField> applied = ConfigureUtils.filterNotEmpty(fields);
        Assert.assertEquals(1, applied.size());
        RuntimeConfigure configure = ConfigureUtils.preparedConfigure(applied);
        Assert.assertEquals("127.0.0.1", configure.getHost());
    }

    @Test
    public void testNullArrayConfiguresGiveNoEnvironment()
    {
        List<PluginConfigureField> fields = Arrays.asList(
                PluginConfigureField.builder().field(PluginConfigureFieldName.configures).type(PluginConfigureFieldType.Array).build());

        Assert.assertTrue(ConfigureUtils.getMapValue(fields, PluginConfigureFieldName.configures).isEmpty());
    }
}
