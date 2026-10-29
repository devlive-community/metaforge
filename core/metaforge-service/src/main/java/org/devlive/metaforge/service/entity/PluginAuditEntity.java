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
import org.devlive.metaforge.service.persistence.Reference;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@NoArgsConstructor
@Table("metaforge_source_query")
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC", "EQ_DOESNT_OVERRIDE_EQUALS"},
        justification = "I prefer to suppress these FindBugs warnings")
public class PluginAuditEntity
        extends BaseEntity
{
    @Column("state")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private State state;

    @Column("content")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String content;

    @Column("message")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String message;

    @Column("elapsed")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Long elapsed;

    @Column("count")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private int count;

    @Column("query_mode")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private QueryMode mode;

    @Column("home")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String home;

    @Column("format")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String format;

    @Column("source_id")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Long sourceId;

    @Transient
    @Reference("sourceId")
    @JsonIncludeProperties(value = {"name", "type", "code"})
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private SourceEntity source;

    // Add from 1.1.0.20221115
    @Column("user_id")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Long userId;

    @Transient
    @Reference("userId")
    @JsonIncludeProperties(value = {"id", "username"})
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private UserEntity user;
}
