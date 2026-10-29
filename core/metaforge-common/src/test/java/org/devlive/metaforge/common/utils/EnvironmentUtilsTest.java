package org.devlive.metaforge.common.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class EnvironmentUtilsTest
{
    @Test
    public void recognizesModernIntellijRuntimeAgent()
    {
        Assert.assertTrue(EnvironmentUtils.isIntelliJEnvironment("app/classes", "", "org.devlive.metaforge.server.MetaForge",
                List.of("-javaagent:/Applications/IntelliJ IDEA.app/Contents/lib/idea_rt.jar=54731")));
        Assert.assertTrue(EnvironmentUtils.isIntelliJEnvironment("", "", "",
                List.of("-javaagent:C:\\Program Files\\JetBrains\\IntelliJ IDEA\\lib\\idea_rt.jar=54731")));
    }

    @Test
    public void retainsLegacyIntellijDetection()
    {
        Assert.assertTrue(EnvironmentUtils.isIntelliJEnvironment("app:idea_rt.jar", "", "", List.of()));
        Assert.assertTrue(EnvironmentUtils.isIntelliJEnvironment("", "jetbrains", "", List.of()));
        Assert.assertTrue(EnvironmentUtils.isIntelliJEnvironment("", "", "com.intellij.rt.execution.application.AppMain", List.of()));
    }

    @Test
    public void productionAndOtherAgentsAreNotIntellij()
    {
        Assert.assertFalse(EnvironmentUtils.isIntelliJEnvironment("app.jar", "", "MetaForge", List.of()));
        Assert.assertFalse(EnvironmentUtils.isIntelliJEnvironment("app.jar", "", "MetaForge",
                List.of("-javaagent:/opt/otel/opentelemetry-javaagent.jar", "-Ddescription=idea_rt.jar")));
    }
}
