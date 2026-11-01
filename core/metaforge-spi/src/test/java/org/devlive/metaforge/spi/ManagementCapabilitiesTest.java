package org.devlive.metaforge.spi;

import org.devlive.metaforge.spi.model.ManagementCapabilities;
import org.devlive.metaforge.spi.model.ManagementCapability;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ManagementCapabilitiesTest
{
    static class DeclaringService
    {
    }

    static class InheritingService
            extends DeclaringService
    {
    }

    static class SilentService
    {
    }

    static class BrokenService
    {
    }

    @Test
    public void aPluginDeclaresItsCapabilitiesNextToItsService()
    {
        ManagementCapabilities capabilities = ManagementCapabilities.declaredBy(DeclaringService.class);
        Assert.assertEquals("relational", capabilities.getKind());
        Assert.assertTrue(capabilities.supports("tab:structure"));
        Assert.assertFalse(capabilities.supports("table:create"));
    }

    @Test
    public void aSubclassWithoutItsOwnFileUsesItsParentsDeclaration()
    {
        Assert.assertEquals(ManagementCapabilities.declaredBy(DeclaringService.class).getCapabilities(),
                ManagementCapabilities.declaredBy(InheritingService.class).getCapabilities());
    }

    @Test
    public void noDeclarationMeansNoManagement()
    {
        ManagementCapabilities capabilities = ManagementCapabilities.declaredBy(SilentService.class);
        Assert.assertEquals("unknown", capabilities.getKind());
        Assert.assertTrue(capabilities.getCapabilities().isEmpty());
    }

    @Test(expected = IllegalStateException.class)
    public void anUnknownCapabilityIsAPluginError()
    {
        ManagementCapabilities.declaredBy(BrokenService.class);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void callersCannotMutateAPluginCapabilitySet()
    {
        ManagementCapabilities.declaredBy(DeclaringService.class).getCapabilities().clear();
    }

    @Test
    public void everyCapabilityKeyIsNamespaced()
    {
        ManagementCapability.keys().forEach(key -> Assert.assertTrue(key, key.matches("[a-z-]+:[a-z-]+")));
    }

    /** Every plugin of the repository ships a valid declaration named after an existing service class. */
    @Test
    public void everyPluginDeclarationIsValid()
            throws IOException
    {
        Path plugins = Paths.get("../../plugin").toAbsolutePath().normalize();
        Assert.assertTrue(plugins.toString(), Files.isDirectory(plugins));
        List<Path> files;
        try (Stream<Path> walk = Files.walk(plugins)) {
            files = walk.filter(path -> path.toString().endsWith(".capabilities.json") && path.toString().contains("src/main/resources"))
                    .collect(Collectors.toList());
        }
        Assert.assertFalse(files.isEmpty());
        for (Path file : files) {
            try (InputStream stream = Files.newInputStream(file)) {
                ManagementCapabilities capabilities = ManagementCapabilities.read(file.toString(), stream);
                if (!capabilities.getCapabilities().isEmpty()) {
                    Assert.assertTrue(file + " can not run SQL", capabilities.supports("query:execute"));
                }
            }
            String resource = file.toString();
            String source = resource.replace("src/main/resources", "src/main/java").replace(".capabilities.json", ".java");
            String kotlin = resource.replace("src/main/resources", "src/main/kotlin").replace(".capabilities.json", ".kt");
            Assert.assertTrue(file + " names no service class", Files.exists(Paths.get(source)) || Files.exists(Paths.get(kotlin)));
        }
    }
}
