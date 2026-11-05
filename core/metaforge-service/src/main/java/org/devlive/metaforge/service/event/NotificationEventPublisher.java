package org.devlive.metaforge.service.event;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.enums.EntityType;
import org.devlive.metaforge.service.enums.NotificationScene;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Raises notifications. Delivery happens after the surrounding transaction commits, so a change
 * that rolls back notifies nobody, and it never fails the caller.
 */
@Service
@RequiredArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The event publisher is a Spring-managed singleton")
public class NotificationEventPublisher
{
    private final ApplicationEventPublisher eventPublisher;

    public void publish(NotificationEvent event)
    {
        if (event != null && event.getUser() != null && event.getScene() != null) {
            eventPublisher.publishEvent(event);
        }
    }

    /** Notifies the signed-in user about an entity. */
    public void publish(NotificationScene scene, String title, String content, EntityType entityType, String entityCode, String entityName)
    {
        publish(scene, currentUser(), title, content, entityType, entityCode, entityName);
    }

    public void publish(NotificationScene scene, UserEntity user, String title, String content, EntityType entityType, String entityCode, String entityName)
    {
        publish(NotificationEvent.builder()
                .scene(scene)
                .user(user)
                .title(title)
                .content(content)
                .entityType(entityType)
                .entityCode(entityCode)
                .entityName(entityName)
                .entityExists(!scene.name().endsWith("_DELETED"))
                .build());
    }

    private static UserEntity currentUser()
    {
        try {
            return UserDetailsService.getUser();
        }
        catch (RuntimeException e) {
            return null;
        }
    }
}
