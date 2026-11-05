package org.devlive.metaforge.service.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.body.adhoc.Adhoc;
import org.devlive.metaforge.service.entity.DataSetEntity;
import org.devlive.metaforge.service.entity.ReportEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.enums.EntityType;
import org.devlive.metaforge.service.enums.NotificationScene;
import org.devlive.metaforge.service.enums.ReportType;
import org.devlive.metaforge.service.event.NotificationEventPublisher;
import org.devlive.metaforge.service.repository.DataSetRepository;
import org.devlive.metaforge.service.repository.ReportRepository;
import org.devlive.metaforge.service.repository.UserRepository;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.devlive.metaforge.service.service.DataSetService;
import org.devlive.metaforge.service.service.ReportService;
import org.devlive.metaforge.service.service.ReportShareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@Slf4j
@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Collaborators are Spring-managed singletons")
public class ReportShareServiceImpl
        implements ReportShareService
{
    static final Pattern TOKEN = Pattern.compile("^[A-Za-z0-9_-]{32}$");
    private static final Pattern ORIGIN = Pattern.compile("^https?://(\\*\\.)?[a-z0-9-]+(\\.[a-z0-9-]+)*(:\\d{1,5})?$");
    private static final int MAX_ORIGINS = 20;
    // Public traffic must not turn into one database query per page view.
    private static final long CACHE_MILLIS = 30_000;
    private static final int CACHE_SIZE = 1000;
    static final int MAX_TABLE_ROWS = 10_000;

    private final ReportRepository repository;
    private final UserRepository userRepository;
    private final DataSetRepository dataSetRepository;
    private final ReportService reportService;
    private final DataSetService dataSetService;
    private final ObjectMapper mapper;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    public ReportShareServiceImpl(ReportRepository repository, UserRepository userRepository, DataSetRepository dataSetRepository,
            ReportService reportService, DataSetService dataSetService, ObjectMapper mapper)
    {
        this.repository = repository;
        this.userRepository = userRepository;
        this.dataSetRepository = dataSetRepository;
        this.reportService = reportService;
        this.dataSetService = dataSetService;
        this.mapper = mapper;
    }

    private NotificationEventPublisher notifications = NotificationEventPublisher.NONE;

    @Autowired
    public void setNotifications(NotificationEventPublisher notifications)
    {
        this.notifications = notifications;
    }

    @Override
    public CommonResponse<ShareState> share(String code, ShareRequest request)
    {
        Long userId = UserDetailsService.getUser().getId();
        ReportEntity report = userId == null ? null : repository.findByCode(code)
                .filter(value -> Objects.equals(value.getUserId(), userId))
                .orElse(null);
        if (report == null) {
            return CommonResponse.failure("Report not found");
        }
        if (request.isEnabled() && !shareable(report)) {
            return CommonResponse.failure("This report was saved by the earlier chart editor. Save it again from the query page to share it.");
        }
        if (request.isEnabled() && request.getExpiresAt() != null && request.getExpiresAt() <= System.currentTimeMillis()) {
            return CommonResponse.failure("The expiry time must be in the future");
        }
        Optional<String> origins = normalizeOrigins(request.getOrigins());
        if (origins.isEmpty()) {
            return CommonResponse.failure("Allowed sites must be origins such as https://example.com, at most " + MAX_ORIGINS);
        }
        boolean wasShared = report.isShareEnabled();
        if (request.isRotate() || report.getShareToken() == null) {
            report.setShareToken(newToken());
        }
        report.setShareEnabled(request.isEnabled());
        report.setShareEmbed(request.isEmbed());
        report.setShareOrigins(origins.get().isEmpty() ? null : origins.get());
        report.setShareExpiresAt(request.getExpiresAt() == null ? null : new Date(request.getExpiresAt()));
        report.setShareShowTitle(!Boolean.FALSE.equals(request.getShowTitle()));
        report.setShareShowDescription(!Boolean.FALSE.equals(request.getShowDescription()));
        report.setShareAllowData(Boolean.TRUE.equals(request.getAllowData()));
        report.setShareAllowDownload(Boolean.TRUE.equals(request.getAllowDownload()));
        repository.save(report);
        evict(report.getShareToken());
        if (wasShared != request.isEnabled()) {
            notifications.publish(request.isEnabled() ? NotificationScene.REPORT_SHARED : NotificationScene.REPORT_UNSHARED,
                    String.format(request.isEnabled() ? "报表「%s」已开启分享" : "报表「%s」已关闭分享", report.getName()),
                    String.format(request.isEnabled() ? "报表「%s」现在可以通过分享链接访问。" : "报表「%s」的分享链接已失效。", report.getName()),
                    EntityType.REPORT, report.getCode(), report.getName());
        }
        return CommonResponse.success(state(report));
    }

    @Override
    public CommonResponse<PublicReport> view(String token)
    {
        return shared(token)
                .map(report -> CommonResponse.success(new PublicReport(report.getName(),
                        report.isShareShowDescription() ? report.getDescription() : null,
                        report.getType() == null ? null : report.getType().name(), report.isRealtime(), report.getConfigure(), report.isShareEmbed(),
                        report.isShareShowTitle(), report.isShareShowDescription(), report.isShareAllowData(), report.isShareAllowDownload())))
                .orElseGet(() -> CommonResponse.failure("Report not found"));
    }

    @Override
    public CommonResponse<List<Map<String, Object>>> refresh(String token)
    {
        Optional<ReportEntity> report = shared(token).filter(value -> value.getType() == ReportType.QUERY && value.isRealtime());
        if (report.isEmpty()) {
            return CommonResponse.failure("Dynamic chart not found");
        }
        // The owner's own refresh path: read-only transaction, row limit and SQL checks included.
        return cached("refresh:" + token, () -> publicly(() -> asOwner(report.get(), () -> reportService.refreshChart(report.get().getCode()))));
    }

    @Override
    public CommonResponse<Object> data(String token)
    {
        Optional<ReportEntity> report = shared(token).filter(value -> value.getType() == ReportType.DATASET);
        if (report.isEmpty() || report.get().getDatasetId() == null) {
            return CommonResponse.failure("Report not found");
        }
        Optional<DataSetEntity> dataset;
        try {
            dataset = dataSetRepository.findById(report.get().getDatasetId());
        }
        catch (RuntimeException e) {
            log.warn("Shared report dataset lookup failed: {}", e.getClass().getSimpleName());
            return CommonResponse.failure("Report not found");
        }
        if (dataset.isEmpty()) {
            return CommonResponse.failure("Report not found");
        }
        Adhoc query;
        try {
            query = mapper.readValue(report.get().getQuery(), Adhoc.class);
        }
        catch (JsonProcessingException e) {
            return CommonResponse.failure("Report query is invalid");
        }
        return cached("data:" + token, () -> publicly(() -> {
            CommonResponse<?> response = asOwner(report.get(), () -> dataSetService.adhoc(dataset.get().getCode(), query));
            return Boolean.TRUE.equals(response.getStatus()) ? CommonResponse.success(response.getData()) : CommonResponse.failure(String.valueOf(response.getMessage()));
        }));
    }

    @Override
    public CommonResponse<Table> table(String token)
    {
        Optional<ReportEntity> report = shared(token).filter(ReportEntity::isShareAllowData);
        if (report.isEmpty()) {
            return CommonResponse.failure("Report data is not shared");
        }
        return rows(report.get(), token);
    }

    @Override
    public Optional<Map.Entry<String, Table>> download(String token)
    {
        Optional<ReportEntity> report = shared(token).filter(ReportEntity::isShareAllowDownload);
        if (report.isEmpty()) {
            return Optional.empty();
        }
        CommonResponse<Table> table = rows(report.get(), token);
        return Boolean.TRUE.equals(table.getStatus())
                ? Optional.of(Map.entry(StringUtils.defaultIfBlank(report.get().getName(), "report"), table.getData()))
                : Optional.empty();
    }

    /** The same rows the public chart shows: the refreshed or saved chart rows, or the dataset result. */
    private CommonResponse<Table> rows(ReportEntity report, String token)
    {
        if (report.getType() == ReportType.DATASET) {
            CommonResponse<Object> data = data(token);
            if (!Boolean.TRUE.equals(data.getStatus())) {
                return CommonResponse.failure(String.valueOf(data.getMessage()));
            }
            JsonNode result = mapper.valueToTree(data.getData());
            List<String> headers = new ArrayList<>();
            result.path("headers").forEach(header -> headers.add(header.asText()));
            List<JsonNode> items = new ArrayList<>();
            result.path("columns").forEach(items::add);
            return CommonResponse.success(table(headers, items));
        }
        List<JsonNode> items = new ArrayList<>();
        if (report.isRealtime()) {
            CommonResponse<List<Map<String, Object>>> refreshed = refresh(token);
            if (Boolean.TRUE.equals(refreshed.getStatus())) {
                refreshed.getData().forEach(row -> items.add(mapper.valueToTree(row)));
            }
        }
        if (items.isEmpty()) {
            try {
                mapper.readTree(report.getConfigure()).path("columns").forEach(items::add);
            }
            catch (JsonProcessingException e) {
                return CommonResponse.failure("Report data is unavailable");
            }
        }
        Set<String> headers = new LinkedHashSet<>();
        items.stream().filter(JsonNode::isObject).forEach(row -> row.fieldNames().forEachRemaining(headers::add));
        return CommonResponse.success(table(new ArrayList<>(headers), items));
    }

    /** Rows as lists in header order; object rows by name, array rows by position. */
    static Table table(List<String> headers, List<JsonNode> items)
    {
        List<List<Object>> rows = new ArrayList<>();
        for (JsonNode item : items) {
            if (rows.size() >= MAX_TABLE_ROWS) {
                return new Table(headers, rows, true);
            }
            List<Object> row = new ArrayList<>(headers.size());
            for (int index = 0; index < headers.size(); index++) {
                JsonNode cell = item.isObject() ? item.get(headers.get(index)) : item.isArray() ? item.get(index) : (index == 0 ? item : null);
                row.add(cell == null || cell.isNull() ? null : cell.isNumber() ? cell.numberValue() : cell.isBoolean() ? cell.booleanValue() : cell.isValueNode() ? cell.asText() : cell.toString());
            }
            rows.add(row);
        }
        return new Table(headers, rows, false);
    }

    @Override
    public Optional<String> frameAncestors(String token)
    {
        return shared(token)
                .filter(ReportEntity::isShareEmbed)
                .map(report -> StringUtils.isBlank(report.getShareOrigins()) ? "*"
                        : "'self' " + String.join(" ", report.getShareOrigins().split("\n")));
    }

    Optional<ReportEntity> shared(String token)
    {
        if (token == null || !TOKEN.matcher(token).matches()) {
            return Optional.empty();
        }
        try {
            return repository.findByShareToken(token)
                    .filter(report -> report.isActive() && report.isShareEnabled() && token.equals(report.getShareToken())
                            && !expired(report, System.currentTimeMillis()) && shareable(report));
        }
        catch (RuntimeException e) {
            // Anonymous callers never see internal errors such as SQL text; to them the report is unavailable.
            log.warn("Shared report lookup failed: {}", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    /** Public reads report a generic failure instead of internal error messages. */
    private static <T> CommonResponse<T> publicly(Supplier<CommonResponse<T>> action)
    {
        try {
            return action.get();
        }
        catch (RuntimeException e) {
            log.warn("Shared report read failed: {}", e.getClass().getSimpleName());
            return CommonResponse.failure("Report data is temporarily unavailable");
        }
    }

    static boolean expired(ReportEntity report, long now)
    {
        return report.getShareExpiresAt() != null && report.getShareExpiresAt().getTime() <= now;
    }

    /** Dataset reports and query charts render from saved data; earlier query reports ran SQL in the browser. */
    boolean shareable(ReportEntity report)
    {
        if (report.getType() == ReportType.DATASET) {
            return true;
        }
        try {
            JsonNode configure = report.getConfigure() == null ? null : mapper.readTree(report.getConfigure());
            return configure != null && configure.path("queryChart").isObject();
        }
        catch (JsonProcessingException e) {
            return false;
        }
    }

    /** One origin per line, lower case and de-duplicated; empty when the input is invalid. */
    static Optional<String> normalizeOrigins(String value)
    {
        Set<String> origins = new LinkedHashSet<>();
        for (String item : StringUtils.defaultString(value).split("[\\s,]+")) {
            String origin = StringUtils.removeEnd(item.trim().toLowerCase(Locale.ROOT), "/");
            if (origin.isEmpty()) {
                continue;
            }
            if (!ORIGIN.matcher(origin).matches()) {
                return Optional.empty();
            }
            origins.add(origin);
        }
        return origins.size() > MAX_ORIGINS ? Optional.empty() : Optional.of(String.join("\n", origins));
    }

    private ShareState state(ReportEntity report)
    {
        return new ShareState(report.isShareEnabled(), report.isShareEmbed(), StringUtils.defaultString(report.getShareOrigins()), report.getShareToken(),
                report.getShareExpiresAt() == null ? null : report.getShareExpiresAt().getTime(), report.isShareShowTitle(), report.isShareShowDescription(),
                report.isShareAllowData(), report.isShareAllowDownload());
    }

    private String newToken()
    {
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Runs the saved definition with the owner's identity, so ownership checks apply as for the owner. */
    private <T> T asOwner(ReportEntity report, Supplier<T> action)
    {
        UserEntity owner = userRepository.findById(report.getUserId()).orElse(null);
        SecurityContext previous = SecurityContextHolder.getContext();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        if (owner != null) {
            UserDetailsService principal = new UserDetailsService(owner.getId(), owner.getCode(), owner.getUsername(), "", new ArrayList<>(), null);
            context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, "", principal.getAuthorities()));
        }
        SecurityContextHolder.setContext(context);
        try {
            return action.get();
        }
        finally {
            SecurityContextHolder.setContext(previous);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> CommonResponse<T> cached(String key, Supplier<CommonResponse<T>> loader)
    {
        long now = System.currentTimeMillis();
        Cached hit = cache.get(key);
        if (hit != null && now - hit.at < CACHE_MILLIS) {
            return (CommonResponse<T>) hit.response;
        }
        CommonResponse<T> response = loader.get();
        if (Boolean.TRUE.equals(response.getStatus())) {
            if (cache.size() >= CACHE_SIZE) {
                cache.entrySet().removeIf(entry -> now - entry.getValue().at >= CACHE_MILLIS);
                if (cache.size() >= CACHE_SIZE) {
                    cache.clear();
                }
            }
            cache.put(key, new Cached(response, now));
        }
        return response;
    }

    private void evict(String token)
    {
        cache.keySet().removeIf(key -> key.endsWith(":" + token));
    }

    private static final class Cached
    {
        private final CommonResponse<?> response;
        private final long at;

        private Cached(CommonResponse<?> response, long at)
        {
            this.response = response;
            this.at = at;
        }
    }
}
