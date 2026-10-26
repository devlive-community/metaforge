package org.devlive.metaforge.service.entity;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.devlive.metaforge.common.enums.State;
import org.devlive.metaforge.common.view.EntityView;
import org.devlive.metaforge.service.enums.QueryMode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "metaforge_source_query")
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC", "EQ_DOESNT_OVERRIDE_EQUALS"},
        justification = "I prefer to suppress these FindBugs warnings")
public class PluginAuditEntity
        extends BaseEntity
{
    @Column(name = "state", unique = true)
    @Enumerated(EnumType.STRING)
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private State state;

    @Column(name = "content", unique = true)
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String content;

    @Column(name = "message", unique = true)
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String message;

    @Column(name = "elapsed")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Long elapsed;

    @Column(name = "count")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private int count;

    @Column(name = "query_mode")
    @Enumerated(EnumType.STRING)
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private QueryMode mode;

    @Column(name = "code")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String code;

    @Column(name = "home")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String home;

    @Column(name = "format")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String format;

    @ManyToOne
    @JoinColumn(name = "plugin_id")
    @JsonIncludeProperties(value = {"name", "type", "code"})
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private SourceEntity source;

    // Add from 1.1.0.20221115
    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIncludeProperties(value = {"id", "username"})
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private UserEntity user;
}
