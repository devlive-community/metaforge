package org.devlive.metaforge.service.itransient;

import com.fasterxml.jackson.annotation.JsonView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.devlive.metaforge.common.view.EntityView;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class NotificationTypeEntity
{
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private boolean enabled;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String type;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Set<String> services;
}
