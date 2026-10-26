package org.devlive.metaforge.service.audit;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.devlive.metaforge.common.enums.State;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.common.utils.CodeUtils;
import org.devlive.metaforge.common.utils.DateUtils;
import org.devlive.metaforge.convert.ConvertFilter;
import org.devlive.metaforge.convert.model.ConvertRequest;
import org.devlive.metaforge.convert.model.ConvertResponse;
import org.devlive.metaforge.fs.FsRequest;
import org.devlive.metaforge.fs.FsService;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.service.common.FolderUtils;
import org.devlive.metaforge.service.entity.ExecuteEntity;
import org.devlive.metaforge.service.entity.PluginAuditEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.initializer.InitializerConfigure;
import org.devlive.metaforge.service.repository.PluginAuditRepository;
import org.devlive.metaforge.service.repository.SourceRepository;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.devlive.metaforge.spi.model.Response;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.Objects.requireNonNull;

@Aspect
@Component
@Slf4j
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public class AuditPluginHandler
{
    private final ThreadLocal<PluginAuditEntity> threadLocalPluginAudit = new ThreadLocal<>();

    private final PluginAuditRepository pluginAuditRepository;
    private final SourceRepository sourceRepository;
    private final InitializerConfigure initializer;
    private final PluginManager pluginManager;

    public AuditPluginHandler(PluginAuditRepository pluginAuditRepository, SourceRepository sourceRepository, InitializerConfigure initializer, PluginManager pluginManager)
    {
        this.pluginAuditRepository = pluginAuditRepository;
        this.sourceRepository = sourceRepository;
        this.initializer = initializer;
        this.pluginManager = pluginManager;
    }

    @Pointcut("@annotation(auditPlugin)")
    public void cut(AuditPlugin auditPlugin)
    {
    }

    @Before("cut(auditPlugin)")
    public void doBefore(AuditPlugin auditPlugin)
    {
        PluginAuditEntity pluginAudit = new PluginAuditEntity();
        pluginAudit.setCreateTime(Timestamp.valueOf(LocalDateTime.now()));
        threadLocalPluginAudit.set(pluginAudit);
    }

    @AfterReturning(pointcut = "@annotation(auditPlugin)", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, AuditPlugin auditPlugin, CommonResponse jsonResult)
    {
        PluginAuditEntity pluginAudit = threadLocalPluginAudit.get();
        handlerPlugin(joinPoint, pluginAudit, jsonResult);
    }

    protected void handlerPlugin(final JoinPoint joinPoint, final PluginAuditEntity pluginAudit, CommonResponse<Response> jsonResult)
    {
        try {
            if (jsonResult.getStatus()) {
                pluginAudit.setState(State.SUCCESS);
                pluginAudit.setCount(jsonResult.getData().getColumns().size());
            }
            else {
                pluginAudit.setMessage(jsonResult.getMessage().toString());
                pluginAudit.setState(State.FAILURE);
            }
            if (joinPoint.getArgs().length > 0) {
                ExecuteEntity executeEntity = (ExecuteEntity) joinPoint.getArgs()[0];
                pluginAudit.setContent(executeEntity.getContent());
                pluginAudit.setMode(executeEntity.getMode());
                pluginAudit.setFormat(executeEntity.getFormat());
                this.sourceRepository.findByCode(executeEntity.getName())
                        .ifPresent(pluginAudit::setSource);
            }
            UserEntity user = UserDetailsService.getUser();
            pluginAudit.setUser(user);
            pluginAudit.setUpdateTime(Timestamp.valueOf(LocalDateTime.now()));
            pluginAudit.setElapsed(pluginAudit.getUpdateTime().getTime() - pluginAudit.getCreateTime().getTime());
            pluginAudit.setCode(CodeUtils.generateCode(false));
            this.pluginAuditRepository.save(pluginAudit);

            ExecutorService service = Executors.newSingleThreadExecutor();
            service.submit(() -> {
                String uniqueId = !pluginAudit.getCode().isEmpty() ? pluginAudit.getCode() : pluginAudit.getId().toString();
                String workHome = FolderUtils.getWorkHome(initializer.getDataHome(), user.getCode(), String.join(File.separator, "adhoc", uniqueId));
                log.info("Writer file to folder [ {} ] on [ {} ]", workHome, pluginAudit.getId());
                try {
                    ConvertFilter.filter(pluginManager, pluginAudit.getFormat())
                            .ifPresent(it -> {
                                try {
                                    FileUtils.forceMkdir(new File(workHome));
                                    ConvertRequest request = new ConvertRequest();
                                    request.setHeaders(jsonResult.getData().getHeaders());
                                    request.setColumns(jsonResult.getData().getColumns());
                                    request.setName("result-tmp");
                                    request.setPath(workHome);
                                    ConvertResponse response = it.writer(request);
                                    log.info("Writer file absolute path [ {} ]", response.getPath());

                                    if (Boolean.TRUE.equals(response.getSuccessful())) {
                                        File tempFile = new File(requireNonNull(response.getPath()));
                                        try (InputStream inputStream = Files.newInputStream(tempFile.toPath())) {
                                            FsRequest fsRequest = FsRequest.builder()
                                                    .access(initializer.getFsConfigure().getAccess())
                                                    .secret(initializer.getFsConfigure().getSecret())
                                                    .endpoint(workHome)
                                                    .bucket(initializer.getFsConfigure().getBucket())
                                                    .stream(inputStream)
                                                    .fileName("result")
                                                    .build();
                                            // If it is OSS third-party storage, rebuild the default directory
                                            if (!initializer.getFsConfigure().getType().equals("LocalFs")) {
                                                fsRequest.setEndpoint(initializer.getFsConfigure().getEndpoint());
                                                fsRequest.setFileName(String.join(File.separator, user.getCode(), DateUtils.formatYMD(), String.join(File.separator, "adhoc", uniqueId), "result"));
                                            }

                                            pluginManager.getPlugin(initializer.getFsConfigure().getType())
                                                    .map(v -> {
                                                        FsService fsService = v.getService(FsService.class);
                                                        return fsService.writer(fsRequest);
                                                    });
                                            log.info("Delete temp file [ {} ] on [ {} ] state [ {} ]", tempFile, pluginAudit.getId(), Files.deleteIfExists(tempFile.toPath()));
                                            log.info("Writer file to folder [ {} ] on [ {} ] completed", workHome, pluginAudit.getId());
                                            pluginAudit.setHome(workHome);
                                        }
                                    }
                                    else {
                                        log.warn("Writer file to folder [ {} ] on [ {} ] failed {}", workHome, pluginAudit.getId(), response.getMessage());
                                    }
                                }
                                catch (IOException e) {
                                    log.error("Writer file error", e);
                                }
                            });
                }
                catch (Exception ex) {
                    log.error("Writer file to folder [ {} ] on [ {} ] failed", workHome, pluginAudit.getId(), ex);
                }
                finally {
                    service.shutdownNow();
                    pluginAuditRepository.save(pluginAudit);
                    threadLocalPluginAudit.remove();
                }
            });
        }
        catch (Exception ex) {
            log.error("Audit failed ", ex);
        }
    }
}
