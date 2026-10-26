package org.devlive.metaforge.service.event;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import org.devlive.metaforge.service.entity.BaseEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.enums.EntityType;
import org.devlive.metaforge.service.enums.NotificationType;

@Getter
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class NotificationEvent
{
    private final BaseEntity data;
    private final UserEntity user;
    private final String title;
    private final String content;
    private final EntityType entityType;
    private final NotificationType type;
    private final String[] channels;

    public NotificationEvent(
            BaseEntity data,
            UserEntity user,
            String title,
            String content,
            EntityType entityType,
            NotificationType type,
            String[] channels
    )
    {
        this.data = data;
        this.user = user;
        this.title = title;
        this.content = content;
        this.entityType = entityType;
        this.type = type;
        this.channels = channels;
    }
}
