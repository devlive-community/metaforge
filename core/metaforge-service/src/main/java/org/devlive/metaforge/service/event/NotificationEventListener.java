package org.devlive.metaforge.service.event;

import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.notify.NotifyService;
import org.devlive.metaforge.notify.model.NotifyRequest;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.service.entity.BaseEntity;
import org.devlive.metaforge.service.entity.NotificationEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.enums.NotificationType;
import org.devlive.metaforge.service.itransient.NotificationTypeEntity;
import org.devlive.metaforge.service.repository.NotificationRepository;
import org.devlive.metaforge.service.repository.UserRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public class NotificationEventListener
{
    private static final String INTERNAL_NOTIFICATION_TYPE = "Internal";
    private final NotificationRepository repository;
    private final UserRepository userRepository;
    private final PluginManager pluginManager;

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendNotification(NotificationEvent event)
    {
        try {
            UserEntity loginUser = event.getUser();

            // 处理返回结果并发送默认通知
            processResultAndSendDefaultNotification(event);

            // 发送用户配置的通知
            sendCustomNotifications(loginUser, event);
        }
        catch (Exception e) {
            log.error("Failed to send notification via event: {}", e.getMessage(), e);
        }
    }

    private void processResultAndSendDefaultNotification(NotificationEvent event)
    {
        // 获取当前登录用户
        UserEntity loginUser = event.getUser();

        // 检查用户配置的通知类型中是否包含Internal
        boolean shouldSendInternalNotification = userRepository.findByCode(loginUser.getCode())
                .map(user -> Optional.ofNullable(user.getNotifyConfigure())
                        .orElse(Lists.newArrayList())
                        .stream()
                        .anyMatch(notificationType -> {
                            try {
                                return isNotificationTypeValid(notificationType, event.getType(), INTERNAL_NOTIFICATION_TYPE, event.getData());
                            }
                            catch (Exception e) {
                                log.debug("Error checking notification type: {}", e.getMessage());
                                return false;
                            }
                        }))
                .orElse(false);

        // 如果包含Internal类型，则发送默认的系统内部通知
        if (shouldSendInternalNotification) {
            sendDefaultNotification(event, event.getData());
        }
    }

    private boolean isNotificationTypeValid(
            NotificationTypeEntity notificationType,
            NotificationType eventType,
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
        if (eventType.equals(NotificationType.DYNAMIC)) {
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
            return notificationType.getServices().contains(eventType.name());
        }
    }

    private void sendCustomNotifications(UserEntity loginUser, NotificationEvent event)
    {
        userRepository.findByCode(loginUser.getCode())
                .ifPresent(user -> Optional.ofNullable(user.getNotifyConfigure())
                        .orElse(Lists.newArrayList())
                        .stream()
                        .filter(Objects::nonNull)
                        .filter(NotificationTypeEntity::isEnabled)
                        .forEach(notificationType -> {
                            try {
                                if (notificationType.getServices() != null && notificationType.getType() != null) {
                                    // 获取结果数据
                                    Object data = event.getData();

                                    // 处理DYNAMIC类型的特殊情况
                                    boolean servicesContainsType;
                                    if (event.getType().equals(NotificationType.DYNAMIC) && data != null) {
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
                                        servicesContainsType = notificationType.getServices().contains(event.getType().name());
                                    }

                                    if (servicesContainsType) {
                                        // 处理非Internal类型
                                        if (!INTERNAL_NOTIFICATION_TYPE.equals(notificationType.getType())) {
                                            sendNotificationViaPlugin(notificationType.getType(), event);
                                        }
                                    }
                                }
                            }
                            catch (Exception e) {
                                log.debug("Error processing notification type: {}", e.getMessage());
                            }
                        }));
    }

    private void sendNotificationViaPlugin(String type, NotificationEvent event)
    {
        pluginManager.getPlugin(type)
                .flatMap(plugin -> Optional.ofNullable(plugin.getService(NotifyService.class)))
                .ifPresent(notifyService -> {
                    NotifyRequest request = createNotifyRequest(event);
                    notifyService.send(request);
                });
    }

    private NotifyRequest createNotifyRequest(NotificationEvent event)
    {
        NotifyRequest request = new NotifyRequest();
        request.setTitle(event.getTitle());
        request.setContent(event.getContent());
        return request;
    }

    private void sendDefaultNotification(NotificationEvent event, Object configure)
    {
        boolean isDelete = event.getType().equals(NotificationType.DELETED);

        // 处理字符串类型的配置对象
        if (configure instanceof String) {
            configure = processStringConfigure((String) configure);
        }

        // 构建通知实体
        NotificationEntity entity = buildNotificationEntity(event, configure, isDelete);

        // 根据动态类型设置通知类型
        if (event.getType().equals(NotificationType.DYNAMIC) && configure != null) {
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

    private NotificationEntity buildNotificationEntity(NotificationEvent event, Object configure, boolean isDelete)
    {
        boolean isStringConfigure = (configure instanceof String);

        return NotificationEntity.builder()
                .content(event.getContent())
                .user(event.getUser())
                .type(event.getType().name())
                .name(event.getTitle())
                .entityCode(configure == null ? null : ((BaseEntity) configure).getCode())
                .entityName(configure == null ? null : ((BaseEntity) configure).getName())
                .entityType(event.getEntityType())
                .entityExists(!(isDelete || isStringConfigure))
                .isRead(false)
                .build();
    }
}
