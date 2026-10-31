package org.devlive.metaforge.server.configure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class PluginConfigurationTest
{
    @TempDir
    private Path root;

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "JUnit injects the TempDir after construction")
    private Path manifest(String location)
            throws IOException
    {
        Objects.requireNonNull(root, "TempDir root should be injected");
        Path path = root.resolve(location);
        Files.createDirectories(path.getParent());
        Files.writeString(path, "metaforge-executor-local=executor/metaforge-executor-local/pom.xml\n");
        return path;
    }

    @Test
    public void developmentWithoutConfigLocationUsesRepositoryManifest()
            throws IOException
    {
        Path expected = manifest("configure/etc/conf/plugins.properties");
        assertEquals(expected, new PluginConfiguration(new MockEnvironment()).pluginsDirectory(root, true));
        assertFalse(Files.exists(root.resolve("null")));
    }

    @Test
    public void developmentHonorsFileAndDirectoryConfigurationLocations()
            throws IOException
    {
        Path expected = manifest("custom/plugins.properties");
        Path application = root.resolve("custom/application.properties");
        Files.writeString(application, "");
        for (String location : new String[] {"custom", "optional:file:" + application, "file:" + application.getParent() + "/"}) {
            MockEnvironment environment = new MockEnvironment().withProperty("spring.config.location", location);
            assertEquals(expected, new PluginConfiguration(environment).pluginsDirectory(root, true));
        }
    }

    @Test
    public void additionalLocationAndLocalManifestHavePriority()
            throws IOException
    {
        manifest("configure/etc/conf/plugins.properties");
        manifest("custom/plugins.properties");
        Path expected = manifest("override/plugins.properties");
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.config.location", "custom")
                .withProperty("spring.config.additional-location", "missing,optional:file:override/");
        assertEquals(expected, new PluginConfiguration(environment).pluginsDirectory(root, true));
        Path local = manifest("config/plugins.properties");
        assertEquals(local, new PluginConfiguration(new MockEnvironment()).pluginsDirectory(root, true));
    }

    @Test
    public void productionAlwaysUsesInstalledPluginsAndMissingManifestsDoNotCreateDirectories()
            throws IOException
    {
        PluginConfiguration configuration = new PluginConfiguration(new MockEnvironment());
        assertEquals(root.resolve("plugins"), configuration.pluginsDirectory(root, true));
        assertFalse(Files.exists(root.resolve("plugins")));
        manifest("configure/etc/conf/plugins.properties");
        assertEquals(root.resolve("plugins"), configuration.pluginsDirectory(root, false));
    }
}
