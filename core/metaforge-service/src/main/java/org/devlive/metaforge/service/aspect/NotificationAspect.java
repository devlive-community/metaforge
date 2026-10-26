package org.devlive.metaforge.service.aspect;

import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.notify.NotifyService;
import org.devlive.metaforge.notify.model.NotifyRequest;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.service.annotation.SendNotification;
import org.devlive.metaforge.service.entity.BaseEntity;
import org.devlive.metaforge.service.entity.NotificationEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.enums.NotificationType;
import org.devlive.metaforge.service.itransient.NotificationTypeEntity;
import org.devlive.metaforge.service.repository.NotificationRepository;
import org.devlive.metaforge.service.repository.UserRepository;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public class NotificationAspect
{
    private static final String INTERNAL_NOTIFICATION_TYPE = "Internal";
    private final NotificationRepository repository;
    private final UserRepository userRepository;
    private final PluginManager pluginManager;

    @AfterReturning(pointcut = "@annotation(sendNotification)", returning = "result")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendNotification(
            JoinPoint joinPoint,
            SendNotification sendNotification,
            Object result
    )
    {
        try {
            UserEntity loginUser = UserDetailsService.getUser();

            // 处理返回结果并发送默认通知
            processResultAndSendDefaultNotification(result, sendNotification);

            // 发送用户配置的通知
            sendCustomNotifications(loginUser, sendNotification, result);
        }
        catch (Exception e) {
            log.error("Failed to send notification: {}", e.getMessage(), e);
        }
    }

    private void processResultAndSendDefaultNotification(Object result, SendNotification sendNotification)
    {
        if (result instanceof CommonResponse) {
            @SuppressWarnings("unchecked")
            CommonResponse<BaseEntity> response = (CommonResponse<BaseEntity>) result;

            // 获取当前登录用户
            UserEntity loginUser = UserDetailsService.getUser();

            // 检查用户配置的通知类型中是否包含Internal
            boolean shouldSendInternalNotification = userRepository.findByCode(loginUser.getCode())
                    .map(user -> Optional.ofNullable(user.getNotifyConfigure())
                            .orElse(Lists.newArrayList())
                            .stream()
                            .anyMatch(notificationType -> {
                                try {
                                    return isNotificationTypeValid(notificationType, sendNotification, INTERNAL_NOTIFICATION_TYPE, response.getData());
                                }
                                catch (Exception e) {
                                    log.debug("Error checking notification type: {}", e.getMessage());
                                    return false;
                                }
                            }))
                    .orElse(false);

            // 如果包含Internal类型，则发送默认的系统内部通知
            if (shouldSendInternalNotification) {
                sendDefaultNotification(sendNotification, response.getData());
            }
        }
    }

    private boolean isNotificationTypeValid(
            NotificationTypeEntity notificationType,
            SendNotification sendNotification,
            String type,
            Object data
    )
    {
        if (notificationType == null ||
                !notificationType.isEnabled() ||
                notificationType.getType() == null ||
                !notificationType.getType().equals(type) ||
                notificationType.getServices() == null) {
            return false;
        }

        // 处理DYNAMIC类型的特殊情况
        if (sendNotification.type().equals(NotificationType.DYNAMIC)) {
            // 根据data的ID判断实际操作类型
            if (data instanceof BaseEntity) {
                BaseEntity entity = (BaseEntity) data;
                if (entity.getId() == null) {
                    // 新建操作，检查services中是否包含CREATED
                    return notificationType.getServices().contains(NotificationType.CREATED.name());
                }
                else {
                    // 更新操作，检查services中是否包含UPDATED
                    return notificationType.getServices().contains(NotificationType.UPDATED.name());
                }
            }
            return false;
        }
        else {
            // 对于非DYNAMIC类型，检查具体的通知类型
            return notificationType.getServices().contains(sendNotification.type().name());
        }
    }

    private void sendCustomNotifications(UserEntity loginUser, SendNotification sendNotification, Object result)
    {
        userRepository.findByCode(loginUser.getCode())
                .ifPresent(user -> Optional.ofNullable(user.getNotifyConfigure())
                        .orElse(Lists.newArrayList())
                        .stream()
                        .filter(Objects::nonNull)
                        .filter(NotificationTypeEntity::isEnabled)
                        .forEach(notificationType -> {
                            try {
                                if (notificationType.getServices() != null &&
                                        notificationType.getType() != null) {
                                    // 获取结果数据
                                    Object data = null;
                                    if (result instanceof CommonResponse) {
                                        @SuppressWarnings("unchecked")
                                        CommonResponse<BaseEntity> response = (CommonResponse<BaseEntity>) result;
                                        data = response.getData();
                                    }

                                    // 处理DYNAMIC类型的特殊情况
                                    boolean servicesContainsType;
                                    if (sendNotification.type().equals(NotificationType.DYNAMIC) && data != null) {
                                        BaseEntity entity = (BaseEntity) data;
                                        if (entity.getId() == null) {
                                            // 新建操作，检查services中是否包含CREATED
                                            servicesContainsType = notificationType.getServices().contains(NotificationType.CREATED.name());
                                        }
                                        else {
                                            // 更新操作，检查services中是否包含UPDATED
                                            servicesContainsType = notificationType.getServices().contains(NotificationType.UPDATED.name());
                                        }
                                    }
                                    else {
                                        // 对于非DYNAMIC类型，检查具体的通知类型
                                        servicesContainsType = notificationType.getServices().contains(sendNotification.type().name());
                                    }

                                    if (servicesContainsType) {
                                        // 处理非Internal类型
                                        if (!INTERNAL_NOTIFICATION_TYPE.equals(notificationType.getType())) {
                                            sendNotificationViaPlugin(notificationType.getType(), sendNotification);
                                        }
                                    }
                                }
                            }
                            catch (Exception e) {
                                log.debug("Error processing notification type: {}", e.getMessage());
                            }
                        }));
    }

    private void sendNotificationViaPlugin(String type, SendNotification sendNotification)
    {
        pluginManager.getPlugin(type)
                .flatMap(plugin -> Optional.ofNullable(plugin.getService(NotifyService.class)))
                .ifPresent(notifyService -> {
                    NotifyRequest request = createNotifyRequest(sendNotification);
                    notifyService.send(request);
                });
    }

    private NotifyRequest createNotifyRequest(SendNotification sendNotification)
    {
        NotifyRequest request = new NotifyRequest();
        request.setTitle(sendNotification.title());
        request.setContent(sendNotification.content());
        return request;
    }

    private void sendDefaultNotification(SendNotification sendNotification, Object configure)
    {
        boolean isDelete = sendNotification.type().equals(NotificationType.DELETED);

        // 处理字符串类型的配置对象
        if (configure instanceof String) {
            configure = processStringConfigure((String) configure);
        }

        // 构建通知实体
        NotificationEntity entity = buildNotificationEntity(sendNotification, configure, isDelete);

        // 根据动态类型设置通知类型
        if (sendNotification.type().equals(NotificationType.DYNAMIC) && configure != null) {
            entity.setType(((BaseEntity) configure).getId() == null ?
                    NotificationType.CREATED.name() :
                    NotificationType.UPDATED.name());
        }

        repository.save(entity);
    }

    private BaseEntity processStringConfigure(String configureCode)
    {
        BaseEntity baseEntity = BaseEntity.builder()
                .code(configureCode)
                .build();

        // 尝试获取已通知内容是否包含当前相关信息
        repository.findFirstByEntityCode(configureCode)
                .ifPresentOrElse(
                        entity -> baseEntity.setName(entity.getEntityName()),
                        () -> baseEntity.setName(configureCode)
                );

        return baseEntity;
    }

    private NotificationEntity buildNotificationEntity(SendNotification sendNotification, Object configure, boolean isDelete)
    {
        boolean isStringConfigure = (configure instanceof String);

        return NotificationEntity.builder()
                .content(sendNotification.content())
                .user(UserDetailsService.getUser())
                .type(sendNotification.type().name())
                .name(sendNotification.title())
                .entityCode(configure == null ? null : ((BaseEntity) configure).getCode())
                .entityName(configure == null ? null : ((BaseEntity) configure).getName())
                .entityType(sendNotification.entityType())
                .entityExists(!(isDelete || isStringConfigure))
                .isRead(false)
                .build();
    }
}
