package org.devlive.metaforge.service.event;

import lombok.RequiredArgsConstructor;
import org.devlive.metaforge.service.entity.BaseEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.enums.EntityType;
import org.devlive.metaforge.service.enums.NotificationType;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationEventPublisher
{
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 发布通知事件
     *
     * @param data 相关数据实体
     * @param title 通知标题
     * @param content 通知内容
     * @param entityType 实体类型
     * @param type 通知类型
     * @param channels 通知渠道
     */
    public void publishNotificationEvent(
            BaseEntity data,
            String title,
            String content,
            EntityType entityType,
            NotificationType type,
            String[] channels
    )
    {
        // 获取当前用户
        UserEntity user = UserDetailsService.getUser();

        // 发布事件
        NotificationEvent event = new NotificationEvent(
                data,
                user,
                title,
                content,
                entityType,
                type,
                channels
        );
        eventPublisher.publishEvent(event);
    }
}
