package org.devlive.metaforge.test.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.dockerjava.api.DockerClient;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.convert.ConvertService;
import org.devlive.metaforge.convert.json.JsonConvertService;
import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.PluginConfigure;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.plugin.Service;
import org.devlive.metaforge.plugin.loader.PluginClassLoader;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.junit.Assert;
import org.junit.Assume;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Runs a plugin service the way the server does — {@code execute} through its JDBC driver, rows
 * formatted by the JSON convert plugin — against a database in a container. Tests skip when
 * Docker is not available, so a machine without it still builds.
 */
public final class PluginE2E
{
    private PluginE2E()
    {
    }

    /** Skips the calling test class when no Docker daemon is reachable. */
    public static void assumeDocker()
    {
        // Docker Desktop (Engine 29) refuses docker-java's default API version, so pin one both support.
        if (System.getProperty("api.version") == null && System.getenv("DOCKER_API_VERSION") == null) {
            System.setProperty("api.version", "1.41");
        }
        boolean available;
        try {
            available = DockerClientFactory.instance().isDockerAvailable();
        }
        catch (Throwable error) {
            available = false;
        }
        Assume.assumeTrue("Docker is not available, skipping container tests", available);
    }

    /**
     * Skips the calling test when the image does not exist for the host CPU architecture. Emulated
     * databases (arm64-only images on amd64 runners, and the reverse) start orders of magnitude
     * slower or crash outright, which would otherwise fail the suite after a long wait.
     */
    public static void assumeImageSupportsHost(String image)
    {
        assumeDocker();
        DockerClient client = DockerClientFactory.instance().client();
        String host = normalizeArchitecture(client.infoCmd().exec().getArchitecture());
        String imageArch;
        try {
            String reference = new RemoteDockerImage(DockerImageName.parse(image)).get();
            imageArch = normalizeArchitecture(client.inspectImageCmd(reference).exec().getArch());
        }
        catch (Throwable error) {
            Assume.assumeNoException("Image " + image + " could not be pulled, skipping", error);
            return;
        }
        Assume.assumeTrue("Image " + image + " is " + imageArch + " while the Docker host is " + host
                + ", skipping the emulated start", host.equals(imageArch));
    }

    private static String normalizeArchitecture(String architecture)
    {
        if ("x86_64".equals(architecture)) {
            return "amd64";
        }
        if ("aarch64".equals(architecture)) {
            return "arm64";
        }
        return architecture;
    }

    /** A connection to a container's mapped port, formatting rows as JSON objects keyed by column. */
    public static RuntimeConfigure configure(String host, int port, String username, String password, String database)
    {
        return RuntimeConfigure.builder()
                .host(host)
                .port(port)
                .username(Optional.ofNullable(username))
                .password(Optional.ofNullable(password))
                .database(Optional.ofNullable(database))
                .plugin(JSON_ONLY.json)
                .pluginManager(JSON_ONLY)
                .build();
    }

    /** Asserts the statement succeeded, naming the SQL and the plugin's message otherwise. */
    public static Response ok(String what, Response response)
    {
        Assert.assertNotNull(what, response);
        Assert.assertTrue(what + " failed: " + response.getMessage(), Boolean.TRUE.equals(response.getIsSuccessful()));
        return response;
    }

    /** Values of one column of a successful response, as text. */
    public static List<String> column(Response response, String name)
    {
        return response.getColumns().stream()
                .map(row -> ((JsonNode) row).get(name))
                .map(value -> value == null || value.isNull() ? null : value.asText())
                .collect(Collectors.toList());
    }

    /** The first value of the first row, as text. */
    public static String first(Response response)
    {
        Assert.assertFalse("no rows", response.getColumns().isEmpty());
        JsonNode row = (JsonNode) response.getColumns().get(0);
        return row.elements().next().asText();
    }

    private static final JsonOnlyPluginManager JSON_ONLY = new JsonOnlyPluginManager();

    /** Answers every convert lookup with the JSON convert service, the server's default format. */
    @SuppressFBWarnings(value = {"SE_BAD_FIELD"})
    private static final class JsonOnlyPluginManager
            extends PluginManager
    {
        private final Plugin json = new JsonConvertPlugin();

        JsonOnlyPluginManager()
        {
            super(PluginConfigure.builder().pluginsDir(emptyDirectory()).build());
        }

        @Override
        public Optional<Plugin> getPlugin(String name)
        {
            return Optional.of(json);
        }

        private static java.nio.file.Path emptyDirectory()
        {
            try {
                return Files.createTempDirectory("metaforge-e2e-plugins");
            }
            catch (IOException error) {
                throw new UncheckedIOException(error);
            }
        }
    }

    /**
     * Stands in for the source's plugin too: its class loader is the test's own, where the plugin
     * under test and its JDBC driver are, so connections load the driver as the server does.
     */
    @SuppressFBWarnings(value = {"DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED"})
    private static final class JsonConvertPlugin
            extends Plugin
    {
        JsonConvertPlugin()
        {
            setPluginClassLoader(new PluginClassLoader(new URL[0], PluginE2E.class.getClassLoader(), "e2e", "e2e", true, Set.of()));
        }

        @Override
        public <T extends Service> T getService(Class<T> serviceClass)
        {
            if (serviceClass.isAssignableFrom(ConvertService.class) || serviceClass == ConvertService.class) {
                return serviceClass.cast(new JsonConvertService());
            }
            throw new IllegalArgumentException("Only the convert service is available, not " + serviceClass.getName());
        }
    }
}
