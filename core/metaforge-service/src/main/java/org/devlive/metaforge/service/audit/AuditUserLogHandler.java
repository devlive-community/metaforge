package org.devlive.metaforge.service.audit;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.bitwalker.useragentutils.UserAgent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.devlive.metaforge.common.enums.State;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.entity.UserLogEntity;
import org.devlive.metaforge.service.repository.UserLogRepository;
import org.devlive.metaforge.service.repository.UserRepository;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.http.HttpServletRequest;

import java.sql.Timestamp;

@Aspect
@Component
@Slf4j
@SuppressFBWarnings(value = {"NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", "EI_EXPOSE_REP2"},
        justification = "I prefer to suppress these FindBugs warnings")
public class AuditUserLogHandler
{
    private final UserRepository userRepository;
    private final UserLogRepository userLogRepository;
    private UserLogEntity userLogEntity;

    public AuditUserLogHandler(UserRepository userRepository, UserLogRepository userLogRepository)
    {
        this.userRepository = userRepository;
        this.userLogRepository = userLogRepository;
    }

    @Pointcut("@annotation(auditUserLog)")
    public void cut(AuditUserLog auditUserLog)
    {
    }

    @Before("cut(auditUserLog)")
    public void doBefore(AuditUserLog auditUserLog)
    {
        this.userLogEntity = new UserLogEntity();
        this.userLogEntity.setCreateTime(new Timestamp(System.currentTimeMillis()));
    }

    @AfterReturning(pointcut = "@annotation(auditUserLog)", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, AuditUserLog auditUserLog, CommonResponse jsonResult)
    {
        handlerPlugin(joinPoint, userLogEntity, jsonResult);
    }

    protected void handlerPlugin(final JoinPoint joinPoint, final UserLogEntity userLogEntity, CommonResponse jsonResult)
    {
        try {
            if (jsonResult.getStatus()) {
                userLogEntity.setState(State.SUCCESS);
                userRepository.findByCode(UserDetailsService.getUser().getCode())
                        .ifPresent(userLogEntity::setUser);
            }
            else {
                userLogEntity.setMessage(jsonResult.getMessage().toString());
                userLogEntity.setState(State.FAILURE);
            }
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = (HttpServletRequest) requestAttributes.resolveReference(RequestAttributes.REFERENCE_REQUEST);
            userLogEntity.setIp(this.getIpAddress(request));
            userLogEntity.setUa(request.getHeader("user-agent"));
            UserAgent userAgent = UserAgent.parseUserAgentString(userLogEntity.getUa());
            userLogEntity.setClient(userAgent.getBrowser().getName());
            userLogEntity.setDevice(userAgent.getOperatingSystem().getDeviceType().getName());
            this.userLogRepository.save(userLogEntity);
        }
        catch (Exception ex) {
            log.error("Audit failed ", ex);
        }
    }

    private String getIpAddress(HttpServletRequest request)
    {
        String ip = request.getHeader("X-Forwarded-For");
        if (ObjectUtils.isEmpty(ip) || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ObjectUtils.isEmpty(ip) || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ObjectUtils.isEmpty(ip) || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ObjectUtils.isEmpty(ip) || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED");
        }
        if (ObjectUtils.isEmpty(ip) || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_CLUSTER_CLIENT_IP");
        }
        if (ObjectUtils.isEmpty(ip) || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ObjectUtils.isEmpty(ip) || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_FORWARDED_FOR");
        }
        if (ObjectUtils.isEmpty(ip) || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_FORWARDED");
        }
        if (ObjectUtils.isEmpty(ip) || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_VIA");
        }
        if (ObjectUtils.isEmpty(ip) || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("REMOTE_ADDR");
        }
        if (ObjectUtils.isEmpty(ip) || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip.contains(",")) {
            return ip.split(",")[0];
        }
        else {
            return ip;
        }
    }
}
