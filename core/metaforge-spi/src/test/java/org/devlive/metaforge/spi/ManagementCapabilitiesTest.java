package org.devlive.metaforge.spi;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.spi.model.ManagementCapabilities;
import org.devlive.metaforge.spi.model.ManagementCapability;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

@SuppressFBWarnings(value = {"DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED"}, justification = "Tests load plugin classes from throwaway directories")
public class ManagementCapabilitiesTest
{
    public static class DeclaringService
    {
    }

    public static class InheritingService
            extends DeclaringService
    {
    }

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    /** A classes directory holding the given class and, when given, its capabilities.json. */
    private Path plugin(Class<?> type, String declaration)
            throws IOException
    {
        Path root = folder.newFolder().toPath();
        String name = type.getName().replace('.', '/') + ".class";
        Path target = root.resolve(name);
        Files.createDirectories(requireNonNull(target.getParent()));
        try (InputStream in = type.getClassLoader().getResourceAsStream(name)) {
            Files.write(target, in.readAllBytes());
        }
        if (declaration != null) {
            Path file = root.resolve(ManagementCapabilities.FILE);
            Files.createDirectories(requireNonNull(file.getParent()));
            Files.writeString(file, declaration, StandardCharsets.UTF_8);
        }
        return root;
    }

    private static Class<?> load(Path root, ClassLoader parent, Class<?> type)
            throws Exception
    {
        return Class.forName(type.getName(), false, new URLClassLoader(new URL[] {root.toUri().toURL()}, parent));
    }

    private static final String DECLARED = "{\"kind\": \"relational\", \"capabilities\": [\"query:execute\", \"database:list\", \"table:list\", \"column:read\", \"tab:structure\", \"tab:sql\"]}";

    @Test
    public void aPluginDeclaresItsCapabilitiesInItsJar()
            throws Exception
    {
        Class<?> service = load(plugin(DeclaringService.class, DECLARED), ClassLoader.getPlatformClassLoader(), DeclaringService.class);
        ManagementCapabilities capabilities = ManagementCapabilities.declaredBy(service);
        Assert.assertEquals("relational", capabilities.getKind());
        Assert.assertTrue(capabilities.supports("tab:structure"));
        Assert.assertFalse(capabilities.supports("table:create"));
    }

    @Test
    public void pluginsSharingAPackageReadOnlyTheirOwnFile()
            throws Exception
    {
        Class<?> declaring = load(plugin(DeclaringService.class, DECLARED), ClassLoader.getPlatformClassLoader(), DeclaringService.class);
        Class<?> other = load(plugin(DeclaringService.class, "{\"kind\": \"key-value\", \"capabilities\": [\"query:execute\", \"tab:sql\"]}"),
                ClassLoader.getPlatformClassLoader(), DeclaringService.class);
        Assert.assertEquals("relational", ManagementCapabilities.declaredBy(declaring).getKind());
        Assert.assertEquals("key-value", ManagementCapabilities.declaredBy(other).getKind());
    }

    @Test
    public void aSubclassWhoseJarHasNoFileUsesItsParentsDeclaration()
            throws Exception
    {
        Path parentRoot = plugin(DeclaringService.class, DECLARED);
        URLClassLoader parent = new URLClassLoader(new URL[] {parentRoot.toUri().toURL()}, ClassLoader.getPlatformClassLoader());
        Class<?> inheriting = load(plugin(InheritingService.class, null), parent, InheritingService.class);
        Assert.assertEquals(parent, inheriting.getSuperclass().getClassLoader());
        Assert.assertTrue(ManagementCapabilities.declaredBy(inheriting).supports("table:list"));
    }

    @Test
    public void noDeclarationMeansNoManagement()
            throws Exception
    {
        ManagementCapabilities capabilities = ManagementCapabilities.declaredBy(load(plugin(DeclaringService.class, null), ClassLoader.getPlatformClassLoader(), DeclaringService.class));
        Assert.assertEquals("unknown", capabilities.getKind());
        Assert.assertTrue(capabilities.getCapabilities().isEmpty());
    }

    @Test
    public void anUnknownCapabilityIsAPluginError()
            throws Exception
    {
        Class<?> broken = load(plugin(DeclaringService.class, "{\"kind\": \"relational\", \"capabilities\": [\"query:execute\", \"table:teleport\"]}"),
                ClassLoader.getPlatformClassLoader(), DeclaringService.class);
        Assert.assertThrows(IllegalStateException.class, () -> ManagementCapabilities.declaredBy(broken));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void callersCannotMutateAPluginCapabilitySet()
            throws Exception
    {
        ManagementCapabilities.declaredBy(load(plugin(DeclaringService.class, DECLARED), ClassLoader.getPlatformClassLoader(), DeclaringService.class))
                .getCapabilities().clear();
    }

    @Test
    public void everyCapabilityKeyIsNamespaced()
    {
        ManagementCapability.keys().forEach(key -> Assert.assertTrue(key, key.matches("[a-z-]+:[a-z-]+")));
    }

    /** Every plugin of the repository ships one valid capabilities.json in the plugin package of its resources. */
    @Test
    public void everyPluginDeclarationIsValid()
            throws IOException
    {
        Path plugins = Paths.get("../../plugin").toAbsolutePath().normalize();
        List<Path> modules;
        try (Stream<Path> list = Files.list(plugins)) {
            modules = list.filter(path -> path.getFileName().toString().startsWith("metaforge-plugin-") && Files.exists(path.resolve("pom.xml")))
                    .collect(Collectors.toList());
        }
        Assert.assertFalse(modules.isEmpty());
        for (Path module : modules) {
            Path file = module.resolve("src/main/resources").resolve(ManagementCapabilities.FILE);
            Assert.assertTrue(module.getFileName() + " has no " + ManagementCapabilities.FILE, Files.exists(file));
            try (InputStream stream = Files.newInputStream(file)) {
                ManagementCapabilities capabilities = ManagementCapabilities.read(file.toString(), stream);
                if (!capabilities.getCapabilities().isEmpty()) {
                    Assert.assertTrue(file + " can not run SQL", capabilities.supports("query:execute"));
                }
            }
            try (Stream<Path> walk = Files.walk(module.resolve("src/main/resources"))) {
                Assert.assertEquals(module.getFileName() + " declares capabilities twice", 1,
                        walk.filter(path -> path.getFileName().toString().endsWith("capabilities.json")).count());
            }
        }
    }
}
