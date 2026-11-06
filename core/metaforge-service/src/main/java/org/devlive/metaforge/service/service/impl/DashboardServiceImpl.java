package org.devlive.metaforge.service.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.entity.DashboardEntity;
import org.devlive.metaforge.service.entity.ReportEntity;
import org.devlive.metaforge.service.enums.EntityType;
import org.devlive.metaforge.service.enums.NotificationScene;
import org.devlive.metaforge.service.event.NotificationEventPublisher;
import org.devlive.metaforge.service.persistence.Persistence;
import org.devlive.metaforge.service.repository.BaseRepository;
import org.devlive.metaforge.service.repository.DashboardRepository;
import org.devlive.metaforge.service.repository.ReportRepository;
import org.devlive.metaforge.service.repository.UserRepository;
import org.devlive.metaforge.service.repository.jdbc.LinkRepository;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.devlive.metaforge.service.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Collaborators are Spring-managed singletons")
public class DashboardServiceImpl
        implements DashboardService
{
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final DashboardRepository repository;
    private final ReportRepository reportRepository;
    private final LinkRepository linkRepository;

    public DashboardServiceImpl(DashboardRepository repository, ReportRepository reportRepository, LinkRepository linkRepository)
    {
        this.repository = repository;
        this.reportRepository = reportRepository;
        this.linkRepository = linkRepository;
    }

    private NotificationEventPublisher notifications = NotificationEventPublisher.NONE;
    private UserRepository users;

    @Autowired
    public void setUsers(UserRepository users)
    {
        this.users = users;
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
        if (configure.getId() != null) {
            var existing = repository.findById(configure.getId())
                    .filter(value -> Objects.equals(value.getUserId(), user.getId()))
                    .orElse(null);
            if (existing == null) {
                return CommonResponse.failure("Dashboard not found");
            }
            configure.setCreateTime(existing.getCreateTime());
            configure.setActive(existing.isActive());
            if (configure.getAvatar() == null) {
                configure.setAvatar(existing.getAvatar());
            }
        }
        configure.setUser(user);
        List<Long> reportIds = new ArrayList<>();
        if (configure.getReports() != null) {
            for (ReportEntity requested : configure.getReports()) {
                var report = reportRepository.findByCode(requested.getCode())
                        .filter(value -> Objects.equals(value.getUserId(), user.getId()))
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
        return CommonResponse.success(saved);
    }

    @Override
    public CommonResponse<DashboardEntity> getById(BaseRepository<DashboardEntity, Long> baseRepository, Long id)
    {
        return loadOwned(repository.findById(id));
    }

    @Override
    public CommonResponse<DashboardEntity> getByCode(BaseRepository<DashboardEntity, Long> baseRepository, String code)
    {
        return loadOwned(repository.findByCode(code));
    }

    private CommonResponse<DashboardEntity> loadOwned(Optional<DashboardEntity> match)
    {
        Long userId = UserDetailsService.getUser().getId();
        return match.filter(value -> userId != null && Objects.equals(value.getUserId(), userId))
                .map(value -> Persistence.references().load(value))
                .map(value -> withReports(CommonResponse.success(value)))
                .orElseGet(() -> CommonResponse.failure("Dashboard not found"));
    }

    @Override
    @Transactional
    public CommonResponse<String> deleteByCode(BaseRepository<DashboardEntity, Long> baseRepository, String code)
    {
        Long userId = UserDetailsService.getUser().getId();
        if (userId == null) {
            return CommonResponse.failure("Dashboard not found");
        }
        DashboardEntity dashboard = repository.findByCode(code)
                .filter(value -> Objects.equals(value.getUserId(), userId))
                .orElse(null);
        if (dashboard == null) {
            return CommonResponse.failure("Dashboard not found");
        }
        linkRepository.deleteDashboard(dashboard.getId());
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
            DashboardEntity dashboard = repository.findByCode(code)
                    .filter(value -> Objects.equals(value.getUserId(), userId))
                    .orElse(null);
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

    private CommonResponse<DashboardEntity> withReports(CommonResponse<DashboardEntity> response)
    {
        DashboardEntity dashboard = response.getData();
        if (response.getStatus() && dashboard != null) {
            Set<ReportEntity> reports = new HashSet<>();
            reportRepository.findAllById(linkRepository.reportIdsOfDashboard(dashboard.getId())).forEach(report -> {
                if (Objects.equals(report.getUserId(), dashboard.getUserId())) {
                    reports.add(report);
                }
            });
            dashboard.setReports(Persistence.references().load(reports));
        }
        return response;
    }
}
