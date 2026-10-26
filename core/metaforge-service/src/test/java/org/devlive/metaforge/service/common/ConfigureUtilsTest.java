package org.devlive.metaforge.service.common;

import org.devlive.metaforge.service.configure.IConfigure;
import org.devlive.metaforge.service.configure.IConfigureExecutor;
import org.devlive.metaforge.service.configure.IConfigurePipelineType;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ConfigureUtilsTest
{
    @Test
    public void testFindPipelineExecutorSkipsIncompleteEntries()
    {
        // 复现 issue #953：Doris 的 YAML 中存在缺少 executor/type 的 pipeline 条目
        IConfigure configure = new IConfigure();
        List<IConfigureExecutor> pipelines = new ArrayList<>();
        pipelines.add(new IConfigureExecutor());
        configure.setPipelines(pipelines);

        Assert.assertNull(ConfigureUtils.findPipelineExecutor(configure, "TRUNCATE", IConfigurePipelineType.INPUT));
    }

    @Test
    public void testFindPipelineExecutorMatches()
    {
        IConfigure configure = new IConfigure();
        IConfigureExecutor matched = new IConfigureExecutor();
        matched.setExecutor("TRUNCATE");
        matched.setType(IConfigurePipelineType.INPUT);
        List<IConfigureExecutor> pipelines = new ArrayList<>();
        pipelines.add(new IConfigureExecutor());
        pipelines.add(matched);
        configure.setPipelines(pipelines);

        Assert.assertSame(matched, ConfigureUtils.findPipelineExecutor(configure, "TRUNCATE", IConfigurePipelineType.INPUT));
    }

    @Test
    public void testFindPipelineExecutorWithNullPipelines()
    {
        IConfigure configure = new IConfigure();

        Assert.assertNull(ConfigureUtils.findPipelineExecutor(configure, "TRUNCATE", IConfigurePipelineType.INPUT));
    }

    @Test
    public void testFindPipelineExecutorWithNullArguments()
    {
        IConfigure configure = new IConfigure();
        IConfigureExecutor matched = new IConfigureExecutor();
        matched.setExecutor("TRUNCATE");
        matched.setType(IConfigurePipelineType.INPUT);
        List<IConfigureExecutor> pipelines = new ArrayList<>();
        pipelines.add(matched);
        configure.setPipelines(pipelines);

        // 请求参数为 null 时也不能抛 NPE
        Assert.assertNull(ConfigureUtils.findPipelineExecutor(configure, null, null));
        Assert.assertSame(matched, ConfigureUtils.findPipelineExecutor(configure, "TRUNCATE", IConfigurePipelineType.INPUT));
    }
}
