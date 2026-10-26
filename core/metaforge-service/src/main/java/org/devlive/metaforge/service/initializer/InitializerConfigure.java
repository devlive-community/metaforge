package org.devlive.metaforge.service.initializer;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.devlive.metaforge.captcha.entity.ResultEntity;
import org.devlive.metaforge.service.loader.CaptchaCacheLoader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class InitializerConfigure
{
    @Getter
    @Value(value = "${metaforge.registration.enable}")
    private Boolean registrationEnable;

    @Getter
    @Value(value = "${metaforge.captcha.enable}")
    private Boolean captchaEnable;

    @Getter
    @Value(value = "${metaforge.cache.maximum}")
    private Long cacheMaximum;

    @Getter
    @Value(value = "${metaforge.cache.expiration}")
    private Long cacheExpiration;

    @Value(value = "${metaforge.pipeline.maxRunning}")
    private Integer maxRunning;

    @Value(value = "${metaforge.pipeline.maxQueue}")
    private Integer maxQueue;

    @Getter
    @Value(value = "${metaforge.experimental.autoLimit}")
    private Boolean autoLimit;

    @Getter
    @Value(value = "${metaforge.experimental.data}")
    private String dataHome;

    @Getter
    @Value(value = "${metaforge.experimental.avatarPath}")
    private String avatarPath;

    @Getter
    @Value(value = "${metaforge.parser.sql.defaultEngine}")
    private String sqlParserDefaultEngine;

    @Getter
    private LoadingCache<Long, ResultEntity> cache;

    @Getter
    private Map<String, ExecutorService> taskExecutors;

    @Getter
    private final FsConfigure fsConfigure;

    @Getter
    private final DataSetConfigure dataSetConfigure;

    public InitializerConfigure(FsConfigure fsConfigure, DataSetConfigure dataSetConfigure)
    {
        this.fsConfigure = fsConfigure;
        this.dataSetConfigure = dataSetConfigure;
    }

    /**
     * Initializes the function.
     */
    @PostConstruct
    public void init()
    {
        if (ObjectUtils.isEmpty(registrationEnable)) {
            this.registrationEnable = Boolean.TRUE;
        }
        log.info("Metaforge registration enable: {}", this.registrationEnable);
        if (ObjectUtils.isEmpty(captchaEnable)) {
            captchaEnable = Boolean.TRUE;
        }
        log.info("Metaforge captcha enable: {}", this.captchaEnable);

        if (ObjectUtils.isEmpty(cacheMaximum)) {
            cacheMaximum = 10000L;
        }
        log.info("Metaforge cache maximum: {}", this.cacheMaximum);
        if (ObjectUtils.isEmpty(cacheExpiration)) {
            cacheExpiration = 2L;
        }
        log.info("Metaforge cache expiration: {}", this.cacheExpiration);
        if (ObjectUtils.isEmpty(cache) && captchaEnable) {
            cache = CacheBuilder.newBuilder()
                    .expireAfterWrite(cacheExpiration, TimeUnit.MINUTES)
                    .maximumSize(cacheMaximum)
                    .build(new CaptchaCacheLoader());
        }

        if (ObjectUtils.isEmpty(maxRunning)) {
            maxRunning = 100;
        }
        log.info("Metaforge pipeline max running: {}", this.maxRunning);

        if (ObjectUtils.isEmpty(maxQueue)) {
            maxQueue = 200;
        }
        log.info("Metaforge pipeline max queue: {}", this.maxQueue);

        if (ObjectUtils.isEmpty(autoLimit)) {
            autoLimit = Boolean.TRUE;
        }
        log.info("Metaforge experimental auto limit: [ {} ]", this.autoLimit);

        if (this.dataHome.toLowerCase().contains("{user.dir}")) {
            this.dataHome = this.dataHome.replace("{user.dir}", System.getProperty("user.dir"));
        }

        this.avatarPath = String.join(File.separator, this.dataHome, this.avatarPath);
        log.info("Metaforge experimental avatar path: [ {} ]", this.avatarPath);

        log.info("=========== Metaforge experimental fs configure ===========");
        log.info("fs type [ {} ]", fsConfigure.getType());
        log.info("fs access [ {} ]", fsConfigure.getAccess());
        log.info("fs secret [ {} ]", fsConfigure.getSecret());
        log.info("fs endpoint [ {} ]", fsConfigure.getEndpoint());
        log.info("fs bucket [ {} ]", fsConfigure.getBucket());

        log.info("=========== Metaforge sql parser configure ===========");
        log.info("sql parser default engine [ {} ]", this.sqlParserDefaultEngine);

        this.taskExecutors = Maps.newConcurrentMap();
    }
}
