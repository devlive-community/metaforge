package org.devlive.metaforge.plugin;

import org.devlive.metaforge.plugin.loader.PluginClassLoader;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.tools.ToolProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ParallelPluginLoadingTest
{
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    public static final class Probe
    {
        private static final AtomicInteger active = new AtomicInteger();
        private static final AtomicInteger peak = new AtomicInteger();
        private static final AtomicInteger calls = new AtomicInteger();
        private static final Map<String, PluginClassLoader> loaders = new ConcurrentHashMap<>();
        private static final Map<String, Boolean> threads = new ConcurrentHashMap<>();
        private static final List<String> errors = new java.util.concurrent.CopyOnWriteArrayList<>();
        private static CountDownLatch entered;
        private static CountDownLatch release;

        private static void gate(int expected)
        {
            entered = new CountDownLatch(expected);
            release = new CountDownLatch(expected == 0 ? 0 : 1);
        }

        public static void checkContext(Plugin plugin)
        {
            if (Thread.currentThread().getContextClassLoader() != plugin.getPluginClassLoader()
                    || PluginContextManager.getPluginClassLoader() != plugin.getPluginClassLoader()) {
                errors.add(plugin.getClass().getName());
            }
        }

        public static void initialize(Plugin plugin)
        {
            checkContext(plugin);
            loaders.put(plugin.getClass().getSimpleName(), plugin.getPluginClassLoader());
            threads.put(Thread.currentThread().getName(), true);
            calls.incrementAndGet();
            peak.accumulateAndGet(active.incrementAndGet(), Math::max);
            entered.countDown();
            try {
                if (!release.await(15, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Test initialization gate timed out");
                }
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Test initialization interrupted", e);
            }
            finally {
                active.decrementAndGet();
            }
        }
    }

    @Before
    public void resetProbe()
    {
        Probe.active.set(0);
        Probe.peak.set(0);
        Probe.calls.set(0);
        Probe.loaders.clear();
        Probe.threads.clear();
        Probe.errors.clear();
        Probe.gate(0);
    }

    @After
    public void workersDoNotLeak()
            throws Exception
    {
        Probe.release.countDown();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (hasLoaderThreads() && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        Assert.assertFalse("Plugin loading workers must terminate", hasLoaderThreads());
        Assert.assertTrue("Plugin contexts must remain isolated", Probe.errors.isEmpty());
    }

    private boolean hasLoaderThreads()
    {
        return Thread.getAllStackTraces().keySet().stream()
                .anyMatch(thread -> thread.isAlive() && thread.getName().startsWith("plugin-loader-"));
    }

    private Path module(Path directory, String name, String key, String extra)
            throws Exception
    {
        Path classes = directory.resolve("target/classes");
        Files.createDirectories(classes);
        Files.writeString(directory.resolve("pom.xml"), "<project/>");
        Path source = directory.resolve(name + ".java");
        Path dependency = directory.resolve("Dependency.java");
        Files.writeString(dependency, "package fixtures.shared; public class Dependency {"
                + "public static String value() { return \"" + key + "\"; }}");
        Files.writeString(source, "package fixtures; public class " + name + " extends org.devlive.metaforge.plugin.Plugin {"
                + "protected void configurePlug() { org.devlive.metaforge.plugin.ParallelPluginLoadingTest.Probe.initialize(this); }"
                + "public String getKey() { return fixtures.shared.Dependency.value(); }"
                + "public org.devlive.metaforge.plugin.configure.PluginConfigure describe() {"
                + "org.devlive.metaforge.plugin.ParallelPluginLoadingTest.Probe.checkContext(this); "
                + (key.equals("broken") ? "throw new NoClassDefFoundError(\"missing dependency\");" : "return null;") + "}"
                + extra + "}");
        Assert.assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "-proc:none", "-classpath", System.getProperty("java.class.path"), "-d", classes.toString(), source.toString(), dependency.toString()));
        Files.writeString(classes.resolve("unopened-marker.txt"), key);
        return directory;
    }

    private PluginManager manager(Path path, int workers)
    {
        return new PluginManager(PluginConfigure.builder().pluginsDir(path).loadThreads(workers).build());
    }

    private void assertParallelStartup(PluginManager manager, int expected)
            throws Exception
    {
        Probe.gate(expected);
        ExecutorService starter = Executors.newSingleThreadExecutor();
        Future<?> started = starter.submit(manager::start);
        try {
            Assert.assertTrue("Independent plugins must initialize concurrently", Probe.entered.await(10, TimeUnit.SECONDS));
            Assert.assertEquals(expected, Probe.active.get());
            Assert.assertFalse("Startup must wait until initialization finishes", started.isDone());
            Assert.assertFalse(manager.isRunning());
            Assert.assertTrue("Unfinished plugins must not be published", manager.getPluginInfos().isEmpty());
            Probe.release.countDown();
            started.get(15, TimeUnit.SECONDS);
            Assert.assertTrue(manager.isRunning());
            Assert.assertEquals(expected, Probe.peak.get());
        }
        finally {
            Probe.release.countDown();
            starter.shutdownNow();
            starter.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    public void compiledDirectoryLoadingIsBoundedAndWaitsForAllPlugins()
            throws Exception
    {
        Path root = temporary.newFolder("plugins").toPath();
        for (int index = 0; index < 5; index++) {
            module(root.resolve("plugin-" + index), "Concurrent" + index + "Plugin", "value" + index, "");
        }
        PluginManager manager = manager(root, 2);
        try {
            assertParallelStartup(manager, 2);
            Assert.assertEquals(5, Probe.calls.get());
            Assert.assertEquals(5, manager.getPluginInfos().size());
            Assert.assertEquals(2, Probe.threads.size());
            for (PluginMetadata metadata : manager.getPluginInfos()) {
                Assert.assertSame(metadata.getClassLoader(), manager.getPluginClassLoader(metadata.getName()).orElseThrow());
            }
            manager.start();
            Assert.assertEquals("Starting twice must not reload plugins", 5, Probe.calls.get());
        }
        finally {
            manager.destroy();
        }
    }

    @Test
    public void nestedManifestsUseTheSamePoolAndDeduplicatePathsAndCycles()
            throws Exception
    {
        Path root = temporary.getRoot().toPath();
        Path first = module(root.resolve("first"), "ManifestFirstPlugin", "first", "");
        module(root.resolve("second"), "ManifestSecondPlugin", "second", "");
        Path manifest = root.resolve("plugins.properties");
        Files.writeString(manifest, "a=first/pom.xml\nb=second/pom.xml\nc=nested.properties\nd=first\n");
        Files.writeString(root.resolve("nested.properties"), "a=plugins.properties\nb=" + first + "/pom.xml\n");
        PluginManager manager = manager(manifest, 2);
        try {
            assertParallelStartup(manager, 2);
            Assert.assertEquals(2, Probe.calls.get());
            Assert.assertEquals(2, manager.getPluginInfos().size());
        }
        finally {
            manager.destroy();
        }
    }

    @Test
    public void packagedSpiPluginsLoadInParallelWithIsolatedDependencies()
            throws Exception
    {
        Path root = temporary.newFolder("installed").toPath();
        for (String key : new String[] {"One", "Two"}) {
            String name = "Packaged" + key + "Plugin";
            Path module = module(temporary.newFolder(key).toPath(), name, key, "");
            Path classes = module.resolve("target/classes");
            Path serviceDirectory = Files.createDirectories(classes.resolve("META-INF/services"));
            Path services = serviceDirectory.resolve("org.devlive.metaforge.plugin.Plugin");
            Files.writeString(services, "fixtures." + name + "\n");
            Path installed = Files.createDirectories(root.resolve(key));
            try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(installed.resolve("plugin.jar")));
                    Stream<Path> files = Files.walk(classes)) {
                for (Path file : files.filter(Files::isRegularFile).collect(Collectors.toList())) {
                    jar.putNextEntry(new JarEntry(classes.relativize(file).toString().replace('\\', '/')));
                    Files.copy(file, jar);
                    jar.closeEntry();
                }
            }
        }
        PluginManager manager = manager(root, 2);
        try {
            assertParallelStartup(manager, 2);
            Assert.assertEquals("One", manager.getPlugin("PackagedOne").orElseThrow().getKey());
            Assert.assertEquals("Two", manager.getPlugin("PackagedTwo").orElseThrow().getKey());
            Assert.assertNotSame(manager.getPluginClassLoader("PackagedOne").orElseThrow(), manager.getPluginClassLoader("PackagedTwo").orElseThrow());
        }
        finally {
            manager.destroy();
        }
    }

    @Test
    public void oneWorkerRetainsSequentialMode()
            throws Exception
    {
        Path root = temporary.newFolder("sequential").toPath();
        module(root.resolve("a"), "SequentialFirstPlugin", "a", "");
        module(root.resolve("b"), "SequentialSecondPlugin", "b", "");
        PluginManager manager = manager(root, 1);
        try {
            manager.start();
            Assert.assertEquals(1, Probe.peak.get());
            Assert.assertEquals(1, Probe.threads.size());
            Assert.assertEquals(2, manager.getPluginInfos().size());
        }
        finally {
            manager.destroy();
        }
    }

    @Test
    public void aBrokenDependencyDoesNotPreventHealthyPluginsFromRegistering()
            throws Exception
    {
        Path root = temporary.newFolder("failures").toPath();
        module(root.resolve("a-broken"), "BrokenDependencyPlugin", "broken", "");
        module(root.resolve("b-healthy"), "HealthyDependencyPlugin", "healthy", "");
        PluginManager manager = manager(root, 2);
        try {
            manager.start();
            Assert.assertFalse(manager.getPlugin("BrokenDependency").isPresent());
            Assert.assertTrue(manager.getPlugin("HealthyDependency").isPresent());
            Assert.assertNull(Probe.loaders.get("BrokenDependencyPlugin").findResource("unopened-marker.txt"));
        }
        finally {
            manager.destroy();
        }
    }

    @Test
    public void duplicateNamesHaveStableOrderAndDoNotCloseTheWinningLoader()
            throws Exception
    {
        Path root = temporary.newFolder("duplicates").toPath();
        String sameName = "public String getName() { return \"Same\"; }";
        module(root.resolve("a"), "DuplicateFirstPlugin", "a", sameName);
        module(root.resolve("b"), "DuplicateSecondPlugin", "b", sameName);
        PluginManager manager = manager(root, 2);
        try {
            manager.start();
            Assert.assertEquals(1, manager.getPluginInfos().size());
            Assert.assertEquals("b", manager.getPlugin("Same").orElseThrow().getKey());
            PluginClassLoader winner = Probe.loaders.get("DuplicateSecondPlugin");
            Assert.assertSame(winner, manager.getPluginClassLoader("Same").orElseThrow());
            Assert.assertNotNull(winner.findResource("unopened-marker.txt"));
            Assert.assertNull(Probe.loaders.get("DuplicateFirstPlugin").findResource("unopened-marker.txt"));
        }
        finally {
            manager.destroy();
        }
    }

    @Test
    public void interruptedStartupDoesNotPublishPartialPlugins()
            throws Exception
    {
        Path root = temporary.newFolder("interrupted").toPath();
        module(root.resolve("a"), "InterruptedFirstPlugin", "a", "");
        module(root.resolve("b"), "InterruptedSecondPlugin", "b", "");
        PluginManager manager = manager(root, 2);
        Probe.gate(2);
        AtomicBoolean interrupted = new AtomicBoolean();
        Thread starter = new Thread(() -> {
            try {
                manager.start();
            }
            catch (IllegalStateException e) {
                interrupted.set(Thread.currentThread().isInterrupted());
            }
        });
        try {
            starter.start();
            Assert.assertTrue(Probe.entered.await(10, TimeUnit.SECONDS));
            starter.interrupt();
            starter.join(5000);
            Assert.assertFalse("Interrupted startup must return", starter.isAlive());
            Assert.assertTrue(interrupted.get());
            Assert.assertFalse(manager.isRunning());
            Assert.assertTrue(manager.getPluginInfos().isEmpty());
            Assert.assertTrue(manager.getPluginClassLoaders().isEmpty());
        }
        finally {
            Probe.release.countDown();
            starter.join(5000);
            manager.destroy();
        }
    }

    @Test
    public void negativeWorkerCountsAreRejected()
    {
        Assert.assertThrows(IllegalArgumentException.class, () -> manager(temporary.getRoot().toPath(), -1));
    }
}
