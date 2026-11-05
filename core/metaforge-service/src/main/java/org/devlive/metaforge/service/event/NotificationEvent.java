package org.devlive.metaforge.service.event;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.enums.EntityType;
import org.devlive.metaforge.service.enums.NotificationScene;

/**
 * Something a user may be notified about: the scene, the user it concerns, a readable title and
 * content, and the entity it links to on the site.
 */
@Getter
@Builder
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "Immutable event carrying entity references")
public class NotificationEvent
{
    private final NotificationScene scene;
    private final UserEntity user;
    private final String title;
    private final String content;
    private final EntityType entityType;
    private final String entityCode;
    private final String entityName;
    /** Whether the entity still exists, so the site can link to it. */
    @Builder.Default
    private final boolean entityExists = true;
}
