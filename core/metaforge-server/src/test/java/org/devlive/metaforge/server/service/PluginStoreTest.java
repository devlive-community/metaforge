package org.devlive.metaforge.server.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginStoreTest
{
    private static final byte[] PACKAGE = "metaforge plugin archive".getBytes(StandardCharsets.UTF_8);

    private HttpServer server;
    private String base;
    private final List<String> requests = new CopyOnWriteArrayList<>();
    private final AtomicReference<String> statsSha = new AtomicReference<>();
    private volatile int statsStatus = 302;

    @BeforeEach
    void start()
            throws Exception
    {
        statsSha.set(sha(PACKAGE));
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        base = "http://127.0.0.1:" + server.getAddress().getPort();
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().toString();
            requests.add(exchange.getRequestMethod() + " " + path);
            if (path.startsWith("/v1/apps/metaforge/plugins/")) {
                if (statsStatus != 302) {
                    exchange.sendResponseHeaders(statsStatus, -1);
                    exchange.close();
                    return;
                }
                String source = path.contains("source=cdn") ? "cdn" : "maven";
                exchange.getResponseHeaders().add("Location", base + "/" + source + "/pkg.tar.gz");
                exchange.getResponseHeaders().add("X-Forge-Attempt-Id", exchange.getRequestHeaders().getFirst("Idempotency-Key"));
                exchange.getResponseHeaders().add("X-Forge-Receipt", "signed-receipt");
                exchange.getResponseHeaders().add("X-Forge-Sha256", statsSha.get());
                exchange.getResponseHeaders().add("X-Forge-Size", String.valueOf(PACKAGE.length));
                exchange.sendResponseHeaders(302, -1);
            }
            else if (path.endsWith("/complete")) {
                byte[] body = "{\"status\":\"confirmed\"}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            }
            else if (path.startsWith("/v1/apps/metaforge/stats")) {
                byte[] body = "{\"plugins\":[{\"pluginId\":\"metaforge-plugin-mysql\",\"totalDownloads\":\"9007199254740993\"}]}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            }
            else if (path.endsWith(".tar.gz")) {
                exchange.sendResponseHeaders(200, PACKAGE.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(PACKAGE);
                }
            }
            else {
                exchange.sendResponseHeaders(404, -1);
            }
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop()
    {
        server.stop(0);
    }

    private PluginStore store(String source, String mirror)
    {
        return new PluginStore(base, "metaforge", source, mirror, HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build(), false);
    }

    private static PluginStore.Package target()
    {
        return PluginStore.Package.builder().key("metaforge-plugin-mysql").version("2026.0.0").url("https://unused.invalid/x.tar.gz").build();
    }

    private static String sha(byte[] bytes)
            throws Exception
    {
        StringBuilder out = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256").digest(bytes)) {
            out.append(String.format("%02x", b));
        }
        return out.toString();
    }

    @Test
    void countedInstallVerifiesThenReports()
            throws IOException
    {
        AtomicReference<byte[]> installed = new AtomicReference<>();
        assertTrue(store("cdn", "").install(target(), (archive, name) -> {
            assertEquals("metaforge-plugin-mysql", name);
            assertTrue(archive.getFileName().toString().endsWith("-bin.tar.gz"), "the installer detects archives by extension");
            installed.set(read(archive));
            return true;
        }));
        assertArrayEquals(PACKAGE, installed.get());
        assertTrue(requests.stream().anyMatch(r -> r.contains("download?source=cdn")));
        assertTrue(requests.contains("GET /cdn/pkg.tar.gz"));
        assertTrue(requests.stream().anyMatch(r -> r.startsWith("POST /v1/apps/metaforge/downloads/") && r.endsWith("/complete")));
    }

    @Test
    void aFailedInstallIsNotReported()
            throws IOException
    {
        assertFalse(store("maven", "").install(target(), (archive, name) -> false));
        assertTrue(requests.stream().noneMatch(r -> r.endsWith("/complete")));
    }

    @Test
    void aTamperedPackageIsRejectedOnEverySource()
    {
        statsSha.set("0".repeat(64));
        assertThrows(PluginStore.StoreException.class, () -> store("maven", "").install(target(), (archive, name) -> true));
        assertTrue(requests.contains("GET /maven/pkg.tar.gz"));
        assertTrue(requests.contains("GET /cdn/pkg.tar.gz"), "the next source is tried before giving up");
        assertTrue(requests.stream().noneMatch(r -> r.endsWith("/complete")));
    }

    @Test
    void aMirrorUsesTheReceiptButDownloadsFromTheMirror()
            throws IOException
    {
        assertTrue(store("mirror", base + "/mirror/").install(target(), (archive, name) -> true));
        assertTrue(requests.contains("GET /mirror/2026.0.0/plugin/metaforge-plugin-mysql-bin.tar.gz"));
        assertTrue(requests.stream().noneMatch(r -> r.startsWith("GET /maven/") || r.startsWith("GET /cdn/")));
        assertTrue(requests.stream().anyMatch(r -> r.endsWith("/complete")));
    }

    @Test
    void anUnknownVersionInstallsDirectlyWithoutCounting()
            throws Exception
    {
        statsStatus = 404;
        PluginStore.Package direct = PluginStore.Package.builder().key("metaforge-plugin-mysql").version("2026.0.0")
                .url(base + "/direct/pkg.tar.gz").sha256(sha(PACKAGE)).size((long) PACKAGE.length).build();
        assertTrue(store("maven", "").install(direct, (archive, name) -> true));
        assertTrue(requests.contains("GET /direct/pkg.tar.gz"));
        assertTrue(requests.stream().noneMatch(r -> r.endsWith("/complete")));
        PluginStore.Package wrong = PluginStore.Package.builder().key("metaforge-plugin-mysql").version("2026.0.0")
                .url(base + "/direct/pkg.tar.gz").sha256("1".repeat(64)).build();
        assertThrows(PluginStore.StoreException.class, () -> store("maven", "").install(wrong, (archive, name) -> true));
    }

    @Test
    void anUnreachableServiceStillInstalls()
            throws IOException
    {
        PluginStore offline = new PluginStore("http://127.0.0.1:1", "metaforge", "maven", "", HttpClient.newHttpClient(), false);
        PluginStore.Package direct = PluginStore.Package.builder().key("metaforge-plugin-mysql").version("2026.0.0").url(base + "/direct/pkg.tar.gz").build();
        assertTrue(offline.install(direct, (archive, name) -> true));
        assertEquals(Map.of(), offline.downloads(List.of("metaforge-plugin-mysql")));
    }

    @Test
    void downloadCountsStayExactAndCached()
    {
        PluginStore store = store("maven", "");
        Map<String, String> counts = store.downloads(List.of("metaforge-plugin-mysql", "metaforge-plugin-unknown", "BAD KEY"));
        assertEquals(Map.of("metaforge-plugin-mysql", "9007199254740993"), counts);
        store.downloads(List.of("metaforge-plugin-mysql", "metaforge-plugin-unknown"));
        assertEquals(1, requests.stream().filter(r -> r.startsWith("GET /v1/apps/metaforge/stats")).count());
    }

    @Test
    void springCreatesTheStoreFromItsConfiguration()
    {
        try (org.springframework.context.annotation.AnnotationConfigApplicationContext context = new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
            context.registerBean(org.springframework.context.support.PropertySourcesPlaceholderConfigurer.class);
            context.registerBean(PluginStore.class);
            context.refresh();
            assertTrue(context.getBean(PluginStore.class).downloads(List.of("BAD KEY")).isEmpty());
        }
    }

    @Test
    void invalidRequestsAreRefusedBeforeAnyDownload()
    {
        PluginStore store = store("maven", "");
        assertThrows(PluginStore.StoreException.class, () -> store.install(PluginStore.Package.builder().key("../etc").version("1").build(), (a, n) -> true));
        assertThrows(PluginStore.StoreException.class, () -> store.install(PluginStore.Package.builder().key("metaforge-plugin-mysql").version("1/../x").build(), (a, n) -> true));
        assertTrue(requests.isEmpty());
    }

    private static byte[] read(Path path)
    {
        try {
            return Files.readAllBytes(path);
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
