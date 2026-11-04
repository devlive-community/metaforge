package org.devlive.metaforge.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;

/**
 * Downloads store packages through the ForgeStats statistics service.
 *
 * <p>The service answers a download with a 302 to the chosen source (Maven Central, the
 * MetaForge CDN) together with a signed receipt and the official SHA-256 and size. The
 * package is verified against those values before installation, and a successful
 * installation is reported so the store can show download counts. A self-hosted mirror
 * uses the receipt and checksums but downloads from the mirror. When the service is
 * unreachable or does not list the version, the package is downloaded directly and
 * verified against the catalogue values, so installing never depends on statistics.</p>
 */
@Slf4j
@Component
public class PluginStore
{
    static final long MAX_PACKAGE_SIZE = 512L * 1024 * 1024;
    private static final Pattern KEY = Pattern.compile("^[a-z0-9][a-z0-9._-]{0,190}$");
    private static final Pattern VERSION = Pattern.compile("^[0-9A-Za-z][0-9A-Za-z.+-]{0,63}$");
    private static final Pattern SHA256 = Pattern.compile("^[0-9a-f]{64}$");
    private static final List<String> SOURCES = List.of("maven", "cdn");
    private static final Duration COUNTS_TTL = Duration.ofSeconds(60);

    private final String statsUrl;
    private final String app;
    private final String source;
    private final String mirror;
    private final HttpClient http;
    // Redirect targets must be HTTPS; tests against a local server relax this.
    private final boolean requireHttps;
    private final ObjectMapper json = new ObjectMapper();
    private final SecureRandom random = new SecureRandom();
    private final Map<String, CachedCount> counts = new ConcurrentHashMap<>();

    // Two constructors: Spring must be told which one to use, or it looks for a no-argument one.
    @Autowired
    public PluginStore(@Value("${plugin.store.stats-url:https://stats.devlive.org}") String statsUrl,
            @Value("${plugin.store.app:metaforge}") String app,
            @Value("${plugin.store.source:maven}") String source,
            @Value("${plugin.store.mirror:}") String mirror)
    {
        this(statsUrl, app, source, mirror, HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(5))
                .build(), true);
    }

    PluginStore(String statsUrl, String app, String source, String mirror, HttpClient http, boolean requireHttps)
    {
        this.requireHttps = requireHttps;
        this.statsUrl = trimSlash(statsUrl);
        this.app = app == null ? "" : app.trim();
        this.source = source == null || source.isBlank() ? "maven" : source.trim().toLowerCase(Locale.ROOT);
        this.mirror = trimSlash(mirror);
        this.http = http;
        if ("mirror".equals(this.source) && this.mirror.isEmpty()) {
            log.warn("plugin.store.source=mirror needs plugin.store.mirror; downloads use the default source");
        }
    }

    /** What the store asks to install: the catalogue entry of one plugin version. */
    @Data
    @Builder
    public static class Package
    {
        private String key;
        private String version;
        private String url;
        private String sha256;
        private Long size;
    }

    @Data
    @Builder
    static class Receipt
    {
        private String attemptId;
        private String token;
        private String sha256;
        private long size;
        private String location;
    }

    /** Thrown when a package cannot be fetched or fails verification. */
    public static class StoreException
            extends IOException
    {
        public StoreException(String message)
        {
            super(message);
        }
    }

    /**
     * Fetches and verifies a package, hands the local archive to {@code installer} and reports a
     * successful installation. The archive is deleted afterwards.
     */
    public boolean install(Package target, BiPredicate<Path, String> installer)
            throws IOException
    {
        if (target.getKey() == null || !KEY.matcher(target.getKey()).matches()) {
            throw new StoreException("Invalid plugin key");
        }
        if (target.getVersion() == null || !VERSION.matcher(target.getVersion()).matches()) {
            throw new StoreException("Invalid plugin version");
        }
        String expectedSha = target.getSha256() == null ? null : target.getSha256().toLowerCase(Locale.ROOT);
        if (expectedSha != null && !SHA256.matcher(expectedSha).matches()) {
            throw new StoreException("Invalid package checksum");
        }
        Path directory = Files.createTempDirectory("metaforge-store-");
        Path archive = directory.resolve(target.getKey() + "-bin.tar.gz");
        try {
            Optional<Receipt> receipt = fetchCounted(target, archive);
            if (receipt.isEmpty()) {
                if (target.getUrl() == null || target.getUrl().isBlank()) {
                    throw new StoreException("The statistics service is unavailable and the catalogue has no package URL");
                }
                download(URI.create(target.getUrl()), archive, expectedSha, target.getSize());
            }
            else if (expectedSha != null && !expectedSha.equals(receipt.get().getSha256())) {
                // The catalogue and the release record disagree; trust neither silently.
                throw new StoreException("The package checksum differs from the store catalogue");
            }
            if (!installer.test(archive, target.getKey())) {
                return false;
            }
            receipt.ifPresent(this::report);
            return true;
        }
        finally {
            deleteQuietly(archive);
            deleteQuietly(directory);
        }
    }

    /** Counted download through the statistics service; empty when it cannot be used. */
    private Optional<Receipt> fetchCounted(Package target, Path archive)
            throws StoreException
    {
        if (statsUrl.isEmpty() || app.isEmpty()) {
            return Optional.empty();
        }
        String attempt = randomHex(16);
        boolean useMirror = "mirror".equals(source) && !mirror.isEmpty();
        List<String> order = new ArrayList<>();
        if (useMirror) {
            order.add("");
        }
        else {
            order.add(SOURCES.contains(source) ? source : "maven");
            SOURCES.stream().filter(name -> !order.contains(name)).forEach(order::add);
        }
        StoreException verification = null;
        for (String name : order) {
            Optional<Receipt> receipt = issue(target, name, attempt);
            if (receipt.isEmpty()) {
                // Unknown version, unreachable service: not counted, but still installable.
                return Optional.empty();
            }
            URI location = useMirror ? URI.create(mirror + "/" + cdnPath(target.getKey(), target.getVersion()))
                    : URI.create(receipt.get().getLocation());
            try {
                download(location, archive, receipt.get().getSha256(), receipt.get().getSize());
                return receipt;
            }
            catch (StoreException e) {
                verification = e;
                log.warn("Plugin {} {} from source [ {} ] failed: {}", target.getKey(), target.getVersion(), name.isEmpty() ? "mirror" : name, e.getMessage());
            }
        }
        throw verification;
    }

    Optional<Receipt> issue(Package target, String sourceName, String attempt)
    {
        String path = String.format("%s/v1/apps/%s/plugins/%s/versions/%s/download%s", statsUrl, app, target.getKey(), target.getVersion(),
                sourceName.isEmpty() ? "" : "?source=" + sourceName);
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(path))
                    .timeout(Duration.ofSeconds(8))
                    .header("Idempotency-Key", attempt)
                    .header("User-Agent", "metaforge-server")
                    .GET()
                    .build();
            HttpResponse<Void> response = http.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() != 302) {
                log.info("Statistics service answered {} for {} {}; downloading without counting", response.statusCode(), target.getKey(), target.getVersion());
                return Optional.empty();
            }
            String id = response.headers().firstValue("X-Forge-Attempt-Id").orElse("");
            String token = response.headers().firstValue("X-Forge-Receipt").orElse("");
            String sha = response.headers().firstValue("X-Forge-Sha256").orElse("");
            long size = Long.parseLong(response.headers().firstValue("X-Forge-Size").orElse("0"));
            String location = response.headers().firstValue("Location").orElse("");
            if (!attempt.equals(id) || token.isEmpty() || !SHA256.matcher(sha).matches() || size <= 0 || size > MAX_PACKAGE_SIZE
                    || !(location.startsWith("https://") || (!requireHttps && location.startsWith("http://")))) {
                log.warn("Statistics service returned an incomplete download for {} {}", target.getKey(), target.getVersion());
                return Optional.empty();
            }
            return Optional.of(Receipt.builder().attemptId(id).token(token).sha256(sha).size(size).location(location).build());
        }
        catch (IOException | IllegalArgumentException e) {
            log.info("Statistics service unavailable for {} {}: {}", target.getKey(), target.getVersion(), e.getMessage());
            return Optional.empty();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    /** Downloads to {@code archive}, enforcing the expected size and SHA-256 when known. */
    void download(URI location, Path archive, String sha256, Long size)
            throws StoreException
    {
        try {
            HttpRequest request = HttpRequest.newBuilder(location)
                    .timeout(Duration.ofMinutes(10))
                    .header("User-Agent", "metaforge-server")
                    .GET()
                    .build();
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                if (response.statusCode() != 200) {
                    throw new StoreException("Package download failed with HTTP " + response.statusCode());
                }
                long limit = size != null && size > 0 ? size : MAX_PACKAGE_SIZE;
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                long total = 0;
                byte[] buffer = new byte[64 * 1024];
                try (OutputStream out = Files.newOutputStream(archive)) {
                    for (int read = body.read(buffer); read != -1; read = body.read(buffer)) {
                        total += read;
                        if (total > limit) {
                            throw new StoreException("The package is larger than its published size");
                        }
                        digest.update(buffer, 0, read);
                        out.write(buffer, 0, read);
                    }
                }
                if (size != null && size > 0 && total != size) {
                    throw new StoreException("The package size differs from its published size");
                }
                String actual = hex(digest.digest());
                if (sha256 != null && !sha256.equals(actual)) {
                    throw new StoreException("The package checksum does not match");
                }
            }
        }
        catch (StoreException e) {
            deleteQuietly(archive);
            throw e;
        }
        catch (IOException | IllegalArgumentException | NoSuchAlgorithmException e) {
            deleteQuietly(archive);
            throw new StoreException("Package download failed: " + e.getMessage());
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            deleteQuietly(archive);
            throw new StoreException("Package download interrupted");
        }
    }

    /** Reports a verified installation; failures only cost one count. */
    void report(Receipt receipt)
    {
        String body = String.format("{\"sha256\":\"%s\",\"size\":%d}", receipt.getSha256(), receipt.getSize());
        URI uri = URI.create(String.format("%s/v1/apps/%s/downloads/%s/complete", statsUrl, app, receipt.getAttemptId()));
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder(uri)
                        .timeout(Duration.ofSeconds(5))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + receipt.getToken())
                        .header("User-Agent", "metaforge-server")
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                        .build();
                int status = http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
                if (status == 200) {
                    return;
                }
                if (status != 429 && status < 500) {
                    log.warn("Statistics service rejected the download report with HTTP {}", status);
                    return;
                }
            }
            catch (IOException e) {
                log.debug("Download report failed: {}", e.getMessage());
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            try {
                Thread.sleep(200L * (attempt + 1));
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        log.warn("Download report for attempt {} was not delivered", receipt.getAttemptId());
    }

    /**
     * Total confirmed downloads per plugin key, as decimal strings. Keys the service does not
     * know, or every key when it is unreachable, are absent: the store shows them as unknown.
     */
    public Map<String, String> downloads(Collection<String> keys)
    {
        Map<String, String> out = new LinkedHashMap<>();
        if (statsUrl.isEmpty() || app.isEmpty()) {
            return out;
        }
        long now = System.nanoTime();
        List<String> missing = new ArrayList<>();
        for (String key : keys) {
            if (key == null || !KEY.matcher(key).matches()) {
                continue;
            }
            CachedCount cached = counts.get(key);
            if (cached != null && now - cached.at < COUNTS_TTL.toNanos()) {
                if (cached.value != null) {
                    out.put(key, cached.value);
                }
            }
            else if (!missing.contains(key)) {
                missing.add(key);
            }
        }
        for (int from = 0; from < missing.size(); from += 100) {
            List<String> batch = missing.subList(from, Math.min(missing.size(), from + 100));
            Map<String, String> fetched = fetchCounts(batch);
            if (fetched == null) {
                break;
            }
            for (String key : batch) {
                String value = fetched.get(key);
                counts.put(key, new CachedCount(value, now));
                if (value != null) {
                    out.put(key, value);
                }
            }
        }
        return out;
    }

    private Map<String, String> fetchCounts(List<String> keys)
    {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(String.format("%s/v1/apps/%s/stats?ids=%s", statsUrl, app, String.join(",", keys))))
                    .timeout(Duration.ofSeconds(5))
                    .header("User-Agent", "metaforge-server")
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return null;
            }
            Map<String, String> out = new LinkedHashMap<>();
            for (JsonNode plugin : json.readTree(response.body()).path("plugins")) {
                String id = plugin.path("pluginId").asText("");
                String total = plugin.path("totalDownloads").asText("");
                if (!id.isEmpty() && total.matches("^\\d+$")) {
                    out.put(id, total);
                }
            }
            return out;
        }
        catch (IOException | IllegalArgumentException e) {
            log.debug("Download counts unavailable: {}", e.getMessage());
            return null;
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /** The CDN layout: {version}/{type}/{key}-bin.tar.gz, shared by self-hosted mirrors. */
    static String cdnPath(String key, String version)
    {
        String directory = "plugin";
        for (String type : List.of("executor", "convert", "fs", "parser", "scheduler")) {
            if (key.startsWith("metaforge-" + type + "-")) {
                directory = type;
                break;
            }
        }
        return version + "/" + directory + "/" + key + "-bin.tar.gz";
    }

    private String randomHex(int bytes)
    {
        byte[] value = new byte[bytes];
        random.nextBytes(value);
        return hex(value);
    }

    private static String hex(byte[] bytes)
    {
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            out.append(Character.forDigit((b >> 4) & 0xf, 16)).append(Character.forDigit(b & 0xf, 16));
        }
        return out.toString();
    }

    private static String trimSlash(String value)
    {
        return value == null ? "" : value.trim().replaceAll("/+$", "");
    }

    private static void deleteQuietly(Path path)
    {
        try {
            Files.deleteIfExists(path);
        }
        catch (IOException e) {
            log.debug("Cannot delete {}", path, e);
        }
    }

    private static final class CachedCount
    {
        private final String value;
        private final long at;

        private CachedCount(String value, long at)
        {
            this.value = value;
            this.at = at;
        }
    }
}
