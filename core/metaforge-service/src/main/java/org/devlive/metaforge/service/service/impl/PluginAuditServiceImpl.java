package org.devlive.metaforge.service.service.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.common.utils.DateUtils;
import org.devlive.metaforge.convert.ConvertService;
import org.devlive.metaforge.convert.model.ConvertRequest;
import org.devlive.metaforge.convert.model.ConvertResponse;
import org.devlive.metaforge.fs.FsRequest;
import org.devlive.metaforge.fs.FsResponse;
import org.devlive.metaforge.fs.FsService;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.service.activity.HeatmapActivity;
import org.devlive.metaforge.service.entity.PluginAuditEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.initializer.InitializerConfigure;
import org.devlive.metaforge.service.itransient.ContributionRadar;
import org.devlive.metaforge.service.persistence.Persistence;
import org.devlive.metaforge.service.repository.PluginAuditRepository;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.devlive.metaforge.service.service.PluginAuditService;
import org.devlive.metaforge.spi.model.Response;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressFBWarnings(value = {"DM_BOXED_PRIMITIVE_FOR_PARSING", "DM_DEFAULT_ENCODING", "EI_EXPOSE_REP2"})
public class PluginAuditServiceImpl
        implements PluginAuditService
{
    private final PluginAuditRepository pluginAuditRepository;
    private final InitializerConfigure initializer;
    private final PluginManager pluginManager;
    private final NamedParameterJdbcOperations jdbc;

    public PluginAuditServiceImpl(PluginAuditRepository pluginAuditRepository, InitializerConfigure initializer, PluginManager pluginManager,
            NamedParameterJdbcOperations jdbc)
    {
        this.jdbc = jdbc;
        this.pluginAuditRepository = pluginAuditRepository;
        this.initializer = initializer;
        this.pluginManager = pluginManager;
    }

    @Override
    public CommonResponse<Long> count()
    {
        return CommonResponse.success(this.pluginAuditRepository.countByUserId(UserDetailsService.getUser().getId()));
    }

    @Override
    public CommonResponse<List<HeatmapActivity>> getAllContribution()
    {
        UserEntity user = UserDetailsService.getUser();
        // Grouped per calendar day in Java: date truncation differs on every database.
        Map<LocalDate, Long> days = new TreeMap<>();
        jdbc.query("SELECT create_time FROM metaforge_source_query WHERE user_id = :userId AND create_time IS NOT NULL",
                Collections.singletonMap("userId", user.getId()),
                (RowCallbackHandler) row -> days.merge(row.getTimestamp(1).toLocalDateTime().toLocalDate(), 1L, Long::sum));
        List<HeatmapActivity> activities = new ArrayList<>();
        days.forEach((day, total) -> activities.add(new HeatmapActivity(total, java.sql.Date.valueOf(day))));
        return CommonResponse.success(activities);
    }

    @Override
    public CommonResponse<List<ContributionRadar>> getContributionRadar()
    {
        UserEntity user = UserDetailsService.getUser();
        List<ContributionRadar> contributionRadars = new ArrayList<>();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", user.getId());
        parameters.put("from", Timestamp.valueOf(LocalDate.now().minusDays(7).atStartOfDay()));
        List<Map<String, Object>> list = jdbc.query("SELECT s.name, COUNT(*) FROM metaforge_source_query q "
                        + "JOIN metaforge_source s ON q.source_id = s.id "
                        + "WHERE q.create_time >= :from AND s.user_id = :userId GROUP BY s.name",
                parameters, (row, index) -> {
                    Map<String, Object> value = new HashMap<>();
                    value.put("dataOfLabel", row.getString(1));
                    value.put("dataOfCount", row.getLong(2));
                    return value;
                });
        Long count = list.stream().mapToLong(v -> Long.valueOf(String.valueOf(v.get("dataOfCount")))).sum();
        list.forEach(v -> {
            ContributionRadar radar = new ContributionRadar();
            radar.setLabel(String.valueOf(v.get("dataOfLabel")));
            radar.setCount(Long.valueOf(String.valueOf(v.get("dataOfCount"))));
            BigDecimal left = new BigDecimal(radar.getCount());
            BigDecimal right = new BigDecimal(count);
            BigDecimal divide = left.divide(right, 2, RoundingMode.HALF_UP);
            radar.setPercentage(Float.valueOf(divide.toString()) * 100);
            contributionRadars.add(radar);
        });
        return CommonResponse.success(contributionRadars);
    }

    @Override
    public CommonResponse<PluginAuditEntity> getById(Long id)
    {
        return pluginAuditRepository.findById(id)
                .map(CommonResponse::success)
                .orElse(CommonResponse.failure(String.format("PluginAudit [ %s ] not found", id)));
    }

    @Override
    public CommonResponse<Response> getData(String code)
    {
        return this.pluginAuditRepository.findByCode(code)
                .map(Persistence.references()::load)
                .map(value -> {
                    Response response = new Response();
                    FsRequest fsRequest = FsRequest.builder()
                            .access(initializer.getFsConfigure().getAccess())
                            .secret(initializer.getFsConfigure().getSecret())
                            .endpoint(value.getHome())
                            .bucket(initializer.getFsConfigure().getBucket())
                            .fileName("result")
                            .build();
                    // If it is OSS third-party storage, rebuild the default directory
                    if (!initializer.getFsConfigure().getType().equals("LocalFs")) {
                        fsRequest.setEndpoint(initializer.getFsConfigure().getEndpoint());
                        fsRequest.setFileName(String.join(File.separator, value.getUser().getCode(), DateUtils.formatYMD(), String.join(File.separator, "adhoc", code), "result"));
                    }
                    pluginManager.getPlugin(initializer.getFsConfigure().getType())
                            .ifPresent(plugin -> {
                                FsService fsService = plugin.getService(FsService.class);
                                FsResponse fsResponse = fsService.reader(fsRequest);

                                if (!fsResponse.isSuccessful()) {
                                    response.setIsSuccessful(false);
                                    response.setMessage(fsResponse.getMessage());
                                    return;
                                }

                                pluginManager.getPlugin(value.getFormat())
                                        .ifPresent(it -> {
                                            ConvertRequest request = new ConvertRequest();
                                            request.setStream(fsResponse.getContext());
                                            ConvertService convertService = it.getService(ConvertService.class);

                                            ConvertResponse _response = convertService.formatStream(request);
                                            if (Boolean.TRUE.equals(_response.getSuccessful())) {
                                                response.setHeaders(_response.getHeaders()
                                                        .stream()
                                                        .map(String::valueOf)
                                                        .collect(Collectors.toList()));
                                                response.setColumns(_response.getColumns());
                                                response.setIsSuccessful(true);
                                            }
                                            else {
                                                response.setIsSuccessful(false);
                                                response.setMessage(_response.getMessage());
                                            }
                                        });
                            });
                    return CommonResponse.success(response);
                })
                .orElseGet(() -> CommonResponse.failure(String.format("Not found [ %s ] history", code)));
    }
}
