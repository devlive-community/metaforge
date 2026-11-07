package org.devlive.metaforge.service.notify;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.notify.model.NotifyRequest;
import org.devlive.metaforge.notify.model.NotifyResponse;
import org.devlive.metaforge.service.entity.NotificationEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.enums.EntityType;
import org.devlive.metaforge.service.enums.NotificationScene;
import org.devlive.metaforge.service.event.NotificationEvent;
import org.devlive.metaforge.service.itransient.NotificationTypeEntity;
import org.devlive.metaforge.service.repository.NotificationRepository;
import org.devlive.metaforge.service.repository.UserRepository;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Delivers one notification to every channel its user subscribed to the scene: the site channel
 * stores it, plugin channels send it with the user's own settings off the calling thread. Failures
 * are logged by outcome only, never with the user's settings, and never reach the caller.
 */
@Slf4j
@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Collaborators are Spring-managed singletons")
public class NotificationDispatcher
{
    private final UserRepository users;
    private final NotificationRepository notifications;
    private final NotificationChannels channels;
    private final ExecutorService senders;

    public NotificationDispatcher(UserRepository users, NotificationRepository notifications, NotificationChannels channels)
    {
        this.users = users;
        this.notifications = notifications;
        this.channels = channels;
        AtomicInteger counter = new AtomicInteger();
        this.senders = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "notify-sender-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    @PreDestroy
    public void close()
    {
        senders.shutdown();
    }

    /** The channels of this user that receive the scene. */
    public static List<String> subscribed(UserEntity user, NotificationScene scene)
    {
        List<String> types = new ArrayList<>();
        if (user == null || scene == null) {
            return types;
        }
        if (scene.isAdmin() && !UserNotifySettings.isAdmin(user)) {
            return types;
        }
        for (NotificationTypeEntity entry : Optional.ofNullable(user.getNotifyConfigure()).orElse(List.of())) {
            if (entry != null && entry.isEnabled() && entry.getType() != null && !types.contains(entry.getType())
                    && NotificationScene.normalize(entry.getServices()).contains(scene.name())) {
                types.add(entry.getType());
            }
        }
        return types;
    }

    public void dispatch(NotificationEvent event)
    {
        if (event == null || event.getUser() == null || event.getScene() == null) {
            return;
        }
        Optional<UserEntity> found = Optional.ofNullable(event.getUser().getCode()).flatMap(users::findByCode);
        if (found.isEmpty()) {
            return;
        }
        UserEntity user = found.get();
        if (user.getRoles() == null) {
            user.setRoles(event.getUser().getRoles());
        }
        for (String type : subscribed(user, event.getScene())) {
            if (NotificationChannels.INTERNAL.equals(type)) {
                try {
                    saveInternal(user, event.getScene(), event.getTitle(), event.getContent(), event);
                }
                catch (RuntimeException e) {
                    log.warn("Saving site notification {} failed: {}", event.getScene(), e.getClass().getSimpleName());
                }
                continue;
            }
            Map<String, String> configure = user.getNotifyConfigure().stream()
                    .filter(entry -> entry != null && type.equals(entry.getType()))
                    .findFirst()
                    .map(NotificationTypeEntity::getConfigure)
                    .orElse(Map.of());
            send(type, event, configure == null ? Map.of() : configure);
        }
    }

    /** Stores a site notification; entity details come from the event when there is one. */
    public void saveInternal(UserEntity user, NotificationScene scene, String title, String content, NotificationEvent event)
    {
        EntityType entityType = event == null ? null : event.getEntityType();
        notifications.save(NotificationEntity.builder()
                .user(user)
                .userId(user.getId())
                .type(scene == null ? "TEST" : scene.name())
                .name(StringUtils.abbreviate(StringUtils.defaultString(title), 255))
                .content(StringUtils.abbreviate(StringUtils.defaultString(content), 2000))
                .entityType(entityType)
                .entityCode(event == null ? null : event.getEntityCode())
                .entityName(event == null ? null : StringUtils.abbreviate(event.getEntityName(), 255))
                .entityExists(event != null && event.isEntityExists() && event.getEntityCode() != null)
                .isRead(false)
                .build());
    }

    private void send(String type, NotificationEvent event, Map<String, String> configure)
    {
        NotifyRequest request = new NotifyRequest();
        request.setTitle(event.getTitle());
        request.setContent(StringUtils.isBlank(event.getContent()) ? event.getTitle() : event.getContent());
        request.setConfigure(configure);
        try {
            senders.submit(() -> {
                try {
                    NotifyResponse response = channels.service(type).map(service -> service.send(request)).orElse(null);
                    if (response == null) {
                        log.debug("Notification channel {} is not available", type);
                    }
                    else if (!Boolean.TRUE.equals(response.getSuccessful())) {
                        log.warn("Notification {} on {} failed: {}", event.getScene(), type, response.getMessage());
                    }
                }
                catch (RuntimeException | LinkageError e) {
                    log.warn("Notification {} on {} failed: {}", event.getScene(), type, e.getClass().getSimpleName());
                }
            });
        }
        catch (RejectedExecutionException e) {
            log.debug("Notification {} on {} skipped while shutting down", event.getScene(), type);
        }
    }
}
