package org.devlive.metaforge.service.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.body.DashboardShareBody;
import org.devlive.metaforge.service.dashboard.share.DashboardViewerDocument;
import org.devlive.metaforge.service.entity.DashboardEntity;
import org.devlive.metaforge.service.entity.ReportEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.enums.EntityType;
import org.devlive.metaforge.service.enums.NotificationScene;
import org.devlive.metaforge.service.enums.SharePermission;
import org.devlive.metaforge.service.event.NotificationEventPublisher;
import org.devlive.metaforge.service.persistence.Persistence;
import org.devlive.metaforge.service.repository.BaseRepository;
import org.devlive.metaforge.service.repository.DashboardRepository;
import org.devlive.metaforge.service.repository.ReportRepository;
import org.devlive.metaforge.service.repository.RoleRepository;
import org.devlive.metaforge.service.repository.UserRepository;
import org.devlive.metaforge.service.repository.jdbc.DashboardShareRepository;
import org.devlive.metaforge.service.repository.jdbc.LinkRepository;
import org.devlive.metaforge.service.security.RunAs;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.devlive.metaforge.service.service.DashboardService;
import org.devlive.metaforge.service.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Data screens with sharing. The owner shares a screen with users, roles or every signed-in user,
 * each with VIEW, EDIT or PUBLISH, and chooses which widgets viewers see. Viewers receive only the
 * published design and the reports their widgets draw; editors work on the draft; only the owner
 * deletes a screen or changes its sharing. Reports always run as the owner, who exposes them.
 */
@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Collaborators are Spring-managed singletons")
public class DashboardServiceImpl
        implements DashboardService
{
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int CANDIDATES = 20;
    private final DashboardRepository repository;
    private final ReportRepository reportRepository;
    private final LinkRepository linkRepository;
    private final SecureRandom random = new SecureRandom();

    public DashboardServiceImpl(DashboardRepository repository, ReportRepository reportRepository, LinkRepository linkRepository)
    {
        this.repository = repository;
        this.reportRepository = reportRepository;
        this.linkRepository = linkRepository;
    }

    private NotificationEventPublisher notifications = NotificationEventPublisher.NONE;
    private UserRepository users;
    private RoleRepository roles;
    private DashboardShareRepository shares;
    private ReportService reports;

    @Autowired
    public void setUsers(UserRepository users)
    {
        this.users = users;
    }

    @Autowired
    public void setRoles(RoleRepository roles)
    {
        this.roles = roles;
    }

    @Autowired
    public void setShares(DashboardShareRepository shares)
    {
        this.shares = shares;
    }

    @Autowired
    public void setReports(@Lazy ReportService reports)
    {
        this.reports = reports;
    }

    @Autowired
    public void setNotifications(NotificationEventPublisher notifications)
    {
        this.notifications = notifications;
    }

    @Override
    @Transactional
    public CommonResponse<DashboardEntity> saveOrUpdate(BaseRepository<DashboardEntity, Long> baseRepository, DashboardEntity configure)
    {
        var user = UserDetailsService.getUser();
        if (user.getId() == null) {
            return CommonResponse.failure("Dashboard not found");
        }
        Long ownerId = user.getId();
        Set<Long> linked = new HashSet<>();
        if (configure.getId() != null) {
            var existing = repository.findById(configure.getId()).orElse(null);
            SharePermission permission = existing == null ? null : access(existing, user.getId());
            if (permission == null || !permission.atLeast(SharePermission.EDIT)) {
                return CommonResponse.failure("Dashboard not found");
            }
            ownerId = existing.getUserId();
            configure.setCreateTime(existing.getCreateTime());
            configure.setActive(existing.isActive());
            if (configure.getAvatar() == null) {
                configure.setAvatar(existing.getAvatar());
            }
            if (!permission.atLeast(SharePermission.PUBLISH)) {
                // Editors save drafts; the published screen stays as the last publisher left it.
                Optional<String> kept = keepPublished(configure.getConfigure(), existing.getConfigure());
                if (kept.isEmpty()) {
                    return CommonResponse.failure("Only users allowed to publish can change the published screen");
                }
                configure.setConfigure(kept.get());
            }
            linked.addAll(linkRepository.reportIdsOfDashboard(existing.getId()));
        }
        // Saving never changes the owner, whoever edits the screen.
        UserEntity owner = new UserEntity();
        owner.setId(ownerId);
        configure.setUser(owner);
        configure.setUserId(ownerId);
        List<Long> reportIds = new ArrayList<>();
        if (configure.getReports() != null) {
            for (ReportEntity requested : configure.getReports()) {
                Long screenOwner = ownerId;
                var report = reportRepository.findByCode(requested.getCode())
                        .filter(value -> Objects.equals(value.getUserId(), screenOwner) || linked.contains(value.getId()))
                        .orElse(null);
                if (report == null) {
                    return CommonResponse.failure("Dashboard report not found");
                }
                reportIds.add(report.getId());
            }
        }
        boolean created = configure.getId() == null;
        DashboardEntity saved = repository.save(configure);
        linkRepository.replaceDashboardReports(saved.getId(), reportIds);
        if (created) {
            notifications.publish(NotificationScene.DASHBOARD_CREATED, String.format("仪表盘「%s」已创建", saved.getName()),
                    String.format("仪表盘「%s」已创建，包含 %d 个报表。", saved.getName(), reportIds.size()), EntityType.DASHBOARD, saved.getCode(), saved.getName());
        }
        saved.setPermission((created ? SharePermission.OWNER : access(saved, user.getId())).name());
        return CommonResponse.success(saved);
    }

    @Override
    public CommonResponse<DashboardEntity> getById(BaseRepository<DashboardEntity, Long> baseRepository, Long id)
    {
        return load(repository.findById(id));
    }

    @Override
    public CommonResponse<DashboardEntity> getByCode(BaseRepository<DashboardEntity, Long> baseRepository, String code)
    {
        return load(repository.findByCode(code));
    }

    /** A screen as the signed-in user may see it: whole for editors, the viewer document for viewers. */
    private CommonResponse<DashboardEntity> load(Optional<DashboardEntity> match)
    {
        Long userId = UserDetailsService.getUser().getId();
        DashboardEntity dashboard = match.orElse(null);
        SharePermission permission = dashboard == null ? null : access(dashboard, userId);
        if (permission == null) {
            return CommonResponse.failure("Dashboard not found");
        }
        Persistence.references().load(dashboard);
        dashboard.setPermission(permission.name());
        if (permission != SharePermission.OWNER) {
            dashboard.setOwnerName(ownerName(dashboard.getUserId()));
        }
        if (permission == SharePermission.VIEW) {
            Optional<String> viewer = DashboardViewerDocument.of(dashboard.getConfigure(), visibleWidgets(dashboard.getId()));
            dashboard.setConfigure(viewer.orElse(null));
            Set<String> codes = viewer.map(DashboardViewerDocument::reportCodes).orElse(Set.of());
            return withReports(CommonResponse.success(dashboard), codes);
        }
        return withReports(CommonResponse.success(dashboard), null);
    }

    @Override
    @Transactional
    public CommonResponse<String> deleteByCode(BaseRepository<DashboardEntity, Long> baseRepository, String code)
    {
        Long userId = UserDetailsService.getUser().getId();
        DashboardEntity dashboard = owned(code, userId).orElse(null);
        if (dashboard == null) {
            return CommonResponse.failure("Dashboard not found");
        }
        linkRepository.deleteDashboard(dashboard.getId());
        if (shares != null) {
            shares.delete(dashboard.getId());
        }
        repository.delete(dashboard);
        if (users != null) {
            users.clearHomePage(code);
        }
        return CommonResponse.success(code);
    }

    @Override
    @Transactional
    public CommonResponse<String> setHomePage(String code)
    {
        Long userId = UserDetailsService.getUser().getId();
        if (userId == null || users == null) {
            return CommonResponse.failure("Dashboard not found");
        }
        if (code != null) {
            DashboardEntity dashboard = repository.findByCode(code).filter(value -> access(value, userId) != null).orElse(null);
            if (dashboard == null) {
                return CommonResponse.failure("Dashboard not found");
            }
            if (!published(dashboard.getConfigure())) {
                return CommonResponse.failure("Publish the data screen before using it as the home page");
            }
        }
        users.updateHomePage(userId, code);
        return CommonResponse.success(code);
    }

    @Override
    public CommonResponse<List<DashboardEntity>> shared()
    {
        Long userId = UserDetailsService.getUser().getId();
        if (userId == null || shares == null) {
            return CommonResponse.success(List.of());
        }
        Map<Long, SharePermission> granted = shares.sharedWith(userId, linkRepository.roleIdsOfUser(userId));
        List<DashboardEntity> result = StreamSupport.stream(repository.findAllById(granted.keySet()).spliterator(), false)
                .filter(dashboard -> !Objects.equals(dashboard.getUserId(), userId))
                .peek(dashboard -> {
                    SharePermission permission = granted.get(dashboard.getId());
                    dashboard.setPermission(permission.name());
                    dashboard.setOwnerName(ownerName(dashboard.getUserId()));
                    if (permission == SharePermission.VIEW) {
                        dashboard.setConfigure(DashboardViewerDocument.of(dashboard.getConfigure(), visibleWidgets(dashboard.getId())).orElse(null));
                    }
                })
                .sorted(Comparator.comparing((DashboardEntity dashboard) -> dashboard.getUpdateTime() == null ? new Date(0) : dashboard.getUpdateTime()).reversed())
                .collect(Collectors.toList());
        return CommonResponse.success(result);
    }

    @Override
    public CommonResponse<List<Map<String, Object>>> refreshChart(String code, String reportCode)
    {
        Long userId = UserDetailsService.getUser().getId();
        DashboardEntity dashboard = repository.findByCode(code).orElse(null);
        SharePermission permission = dashboard == null ? null : access(dashboard, userId);
        if (permission == null || reports == null) {
            return CommonResponse.failure("Dashboard not found");
        }
        ReportEntity report = reportRepository.findByCode(reportCode)
                .filter(value -> Objects.equals(value.getUserId(), dashboard.getUserId()))
                .filter(value -> linkRepository.reportIdsOfDashboard(dashboard.getId()).contains(value.getId()))
                .orElse(null);
        if (report == null) {
            return CommonResponse.failure("Dashboard report not found");
        }
        if (permission == SharePermission.VIEW) {
            Set<String> visible = DashboardViewerDocument.of(dashboard.getConfigure(), visibleWidgets(dashboard.getId()))
                    .map(DashboardViewerDocument::reportCodes).orElse(Set.of());
            if (!visible.contains(reportCode)) {
                return CommonResponse.failure("Dashboard report not found");
            }
        }
        if (permission == SharePermission.OWNER) {
            return reports.refreshChart(reportCode);
        }
        // The owner shares the screen and with it the right to read its reports; they run as the owner.
        UserEntity owner = users == null ? null : users.findById(dashboard.getUserId()).orElse(null);
        return RunAs.user(owner, () -> reports.refreshChart(reportCode));
    }

    @Override
    public CommonResponse<DashboardShareBody> getShare(String code)
    {
        DashboardEntity dashboard = owned(code, UserDetailsService.getUser().getId()).orElse(null);
        if (dashboard == null || shares == null) {
            return CommonResponse.failure("Dashboard not found");
        }
        return CommonResponse.success(shareOf(dashboard.getId()));
    }

    @Override
    @Transactional
    public CommonResponse<DashboardShareBody> saveShare(String code, DashboardShareBody body)
    {
        Long userId = UserDetailsService.getUser().getId();
        DashboardEntity dashboard = owned(code, userId).orElse(null);
        if (dashboard == null || shares == null || body == null) {
            return CommonResponse.failure("Dashboard not found");
        }
        String everyone = null;
        if (StringUtils.isNotBlank(body.getEveryone())) {
            Optional<SharePermission> permission = SharePermission.grantable(body.getEveryone());
            if (permission.isEmpty()) {
                return CommonResponse.failure("Unknown permission " + body.getEveryone());
            }
            everyone = permission.get().name();
        }
        List<DashboardShareRepository.Member> members = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (DashboardShareBody.Member member : body.getMembers() == null ? List.<DashboardShareBody.Member>of() : body.getMembers()) {
            String type = StringUtils.upperCase(StringUtils.trimToEmpty(member.getType()), Locale.ROOT);
            Optional<SharePermission> permission = SharePermission.grantable(member.getPermission());
            boolean known = DashboardShareRepository.USER.equals(type) ? member.getId() != null && !member.getId().equals(userId) && users.existsById(member.getId())
                    : DashboardShareRepository.ROLE.equals(type) && member.getId() != null && roles.existsById(member.getId());
            if (!known || permission.isEmpty()) {
                return CommonResponse.failure("Share with existing users or roles, each with VIEW, EDIT or PUBLISH");
            }
            if (seen.add(type + ":" + member.getId())) {
                members.add(new DashboardShareRepository.Member(type, member.getId(), permission.get().name()));
            }
        }
        String widgets = null;
        if (body.getWidgets() != null) {
            Set<String> available = DashboardViewerDocument.publishedWidgetIds(dashboard.getConfigure());
            List<String> chosen = body.getWidgets().stream().filter(available::contains).distinct().collect(Collectors.toList());
            if (chosen.isEmpty()) {
                return CommonResponse.failure("Choose at least one published widget to share");
            }
            try {
                widgets = MAPPER.writeValueAsString(chosen);
            }
            catch (JsonProcessingException e) {
                return CommonResponse.failure("Choose at least one published widget to share");
            }
        }
        DashboardShareRepository.Share share = shares.find(dashboard.getId()).orElseGet(DashboardShareRepository.Share::new);
        share.setDashboardId(dashboard.getId());
        DashboardShareBody.PublicLink link = body.getPublicLink() == null ? new DashboardShareBody.PublicLink() : body.getPublicLink();
        if (link.isEnabled()) {
            if (!published(dashboard.getConfigure())) {
                return CommonResponse.failure("Publish the data screen before sharing a public link");
            }
            if (link.getExpiresAt() != null && link.getExpiresAt() <= System.currentTimeMillis()) {
                return CommonResponse.failure("The expiry time must be in the future");
            }
        }
        Optional<String> origins = ReportShareServiceImpl.normalizeOrigins(link.getOrigins());
        if (origins.isEmpty()) {
            return CommonResponse.failure("Allowed sites must be origins such as https://example.com, at most " + ReportShareServiceImpl.MAX_ORIGINS);
        }
        if (link.isRotate() || share.getPublicToken() == null) {
            share.setPublicToken(newToken());
        }
        share.setEveryonePermission(everyone);
        share.setWidgets(widgets);
        share.setPublicEnabled(link.isEnabled());
        share.setPublicEmbed(link.isEmbed());
        share.setPublicOrigins(origins.get().isEmpty() ? null : origins.get());
        share.setPublicExpiresAt(link.getExpiresAt() == null ? null : new Date(link.getExpiresAt()));
        shares.save(share);
        shares.replaceMembers(dashboard.getId(), members);
        return CommonResponse.success(shareOf(dashboard.getId()));
    }

    @Override
    public CommonResponse<List<DashboardShareBody.Member>> shareCandidates(String keyword)
    {
        Long userId = UserDetailsService.getUser().getId();
        String text = StringUtils.lowerCase(StringUtils.trimToEmpty(keyword), Locale.ROOT);
        List<DashboardShareBody.Member> result = new ArrayList<>();
        StreamSupport.stream(users.findAll().spliterator(), false)
                .filter(user -> !Objects.equals(user.getId(), userId) && user.isActive())
                .filter(user -> StringUtils.lowerCase(StringUtils.defaultString(user.getUsername()), Locale.ROOT).contains(text))
                .limit(CANDIDATES)
                .forEach(user -> result.add(new DashboardShareBody.Member(DashboardShareRepository.USER, user.getId(), user.getUsername(), null)));
        StreamSupport.stream(roles.findAll().spliterator(), false)
                .filter(role -> StringUtils.lowerCase(StringUtils.defaultString(role.getName()), Locale.ROOT).contains(text))
                .limit(CANDIDATES)
                .forEach(role -> result.add(new DashboardShareBody.Member(DashboardShareRepository.ROLE, role.getId(), role.getName(), null)));
        return CommonResponse.success(result);
    }

    /**
     * What the user may do with a screen: OWNER for its own user, otherwise the highest of a direct
     * grant, a grant to one of the user's roles and the grant to everyone; null when not shared.
     */
    SharePermission access(DashboardEntity dashboard, Long userId)
    {
        if (userId == null || dashboard == null) {
            return null;
        }
        if (Objects.equals(dashboard.getUserId(), userId)) {
            return SharePermission.OWNER;
        }
        if (shares == null || dashboard.getId() == null) {
            return null;
        }
        SharePermission best = shares.find(dashboard.getId()).flatMap(share -> SharePermission.grantable(share.getEveryonePermission())).orElse(null);
        List<Long> roleIds = linkRepository.roleIdsOfUser(userId);
        for (DashboardShareRepository.Member member : shares.members(dashboard.getId())) {
            boolean mine = DashboardShareRepository.USER.equals(member.getType()) && Objects.equals(member.getId(), userId)
                    || DashboardShareRepository.ROLE.equals(member.getType()) && roleIds.contains(member.getId());
            SharePermission granted = mine ? SharePermission.grantable(member.getPermission()).orElse(null) : null;
            if (granted != null && (best == null || granted.atLeast(best))) {
                best = granted;
            }
        }
        return best;
    }

    private Optional<DashboardEntity> owned(String code, Long userId)
    {
        return userId == null ? Optional.empty() : repository.findByCode(code).filter(value -> Objects.equals(value.getUserId(), userId));
    }

    /** Widget ids viewers see, or null for the whole screen. */
    private Set<String> visibleWidgets(Long dashboardId)
    {
        String widgets = shares == null ? null : shares.find(dashboardId).map(DashboardShareRepository.Share::getWidgets).orElse(null);
        if (widgets == null) {
            return null;
        }
        try {
            return new LinkedHashSet<>(MAPPER.readValue(widgets, new TypeReference<List<String>>() {}));
        }
        catch (JsonProcessingException e) {
            // An unreadable selection shows nothing rather than everything.
            return Set.of();
        }
    }

    private DashboardShareBody shareOf(Long dashboardId)
    {
        DashboardShareBody body = new DashboardShareBody();
        Optional<DashboardShareRepository.Share> share = shares.find(dashboardId);
        body.setEveryone(share.map(DashboardShareRepository.Share::getEveryonePermission).orElse(null));
        Set<String> widgets = share.isPresent() ? visibleWidgets(dashboardId) : null;
        body.setWidgets(widgets == null ? null : new ArrayList<>(widgets));
        List<DashboardShareBody.Member> members = new ArrayList<>();
        for (DashboardShareRepository.Member member : shares.members(dashboardId)) {
            String name = DashboardShareRepository.USER.equals(member.getType())
                    ? users.findById(member.getId()).map(UserEntity::getUsername).orElse(null)
                    : roles.findById(member.getId()).map(role -> role.getName()).orElse(null);
            if (name != null) {
                members.add(new DashboardShareBody.Member(member.getType(), member.getId(), name, member.getPermission()));
            }
        }
        body.setMembers(members);
        DashboardShareBody.PublicLink link = new DashboardShareBody.PublicLink();
        share.ifPresent(value -> {
            link.setEnabled(value.isPublicEnabled());
            link.setEmbed(value.isPublicEmbed());
            link.setOrigins(StringUtils.defaultString(value.getPublicOrigins()));
            link.setExpiresAt(value.getPublicExpiresAt() == null ? null : value.getPublicExpiresAt().getTime());
            link.setToken(value.getPublicToken());
        });
        body.setPublicLink(link);
        return body;
    }

    private String ownerName(Long ownerId)
    {
        return users == null || ownerId == null ? null : users.findById(ownerId).map(UserEntity::getUsername).orElse(null);
    }

    private String newToken()
    {
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * The incoming document with the stored published design kept, for editors without the right to
     * publish. Empty when the stored screen predates drafts and cannot be kept apart.
     */
    static Optional<String> keepPublished(String incoming, String stored)
    {
        try {
            JsonNode next = incoming == null ? null : MAPPER.readTree(incoming);
            JsonNode previous = stored == null || stored.isBlank() ? null : MAPPER.readTree(stored);
            if (previous != null && !previous.isNull() && !(previous.isObject() && previous.path("version").asInt() == 3)) {
                return Optional.empty();
            }
            if (next == null || !next.isObject()) {
                return Optional.empty();
            }
            ObjectNode document = (ObjectNode) next;
            JsonNode published = previous == null ? null : previous.get("published");
            if (published == null || published.isNull()) {
                document.remove("published");
            }
            else {
                document.set("published", published);
            }
            return Optional.of(MAPPER.writeValueAsString(document));
        }
        catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    /**
     * Whether a saved document has a published version, read the way the editor reads it: a
     * version 3 document carries {@code published}; older layouts count as published once saved.
     */
    public static boolean published(String configure)
    {
        if (configure == null || configure.isBlank()) {
            return false;
        }
        try {
            JsonNode root = MAPPER.readTree(configure);
            if (root != null && root.isObject() && root.path("version").asInt() == 3) {
                return root.path("published").isObject();
            }
            return root != null && !root.isNull() && !root.isMissingNode();
        }
        catch (JsonProcessingException e) {
            return false;
        }
    }

    /** Links the screen's reports owned by its owner; with {@code only} set, just those codes. */
    private CommonResponse<DashboardEntity> withReports(CommonResponse<DashboardEntity> response, Set<String> only)
    {
        DashboardEntity dashboard = response.getData();
        if (response.getStatus() && dashboard != null) {
            Set<ReportEntity> linked = new HashSet<>();
            reportRepository.findAllById(linkRepository.reportIdsOfDashboard(dashboard.getId())).forEach(report -> {
                if (Objects.equals(report.getUserId(), dashboard.getUserId()) && (only == null || only.contains(report.getCode()))) {
                    linked.add(report);
                }
            });
            dashboard.setReports(Persistence.references().load(linked));
        }
        return response;
    }
}
