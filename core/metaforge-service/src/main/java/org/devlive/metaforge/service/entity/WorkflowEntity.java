package org.devlive.metaforge.service.entity;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.devlive.metaforge.common.view.EntityView;
import org.devlive.metaforge.executor.common.RunState;
import org.devlive.metaforge.service.itransient.configuration.Configuration;
import org.devlive.metaforge.service.persistence.Reference;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@ToString
@NoArgsConstructor
@Table("metaforge_workflow")
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC"})
public class WorkflowEntity
        extends BaseEntity
{
    @Column("state")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private RunState state;

    @Column("message")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String message;

    @Column("work")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String work;

    @Column("elapsed")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Long elapsed;

    @Column("executor")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String executor;

    @Column("configure")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Configuration configure;

    @Column("from_source_id")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Long fromSourceId;

    @Transient
    @Reference("fromSourceId")
    @JsonIncludeProperties(value = {"code", "name", "type"})
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private SourceEntity from;

    @Column("to_source_id")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Long toSourceId;

    @Transient
    @Reference("toSourceId")
    @JsonIncludeProperties(value = {"code", "name", "type"})
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private SourceEntity to;

    @Column("user_id")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Long userId;

    @Transient
    @Reference("userId")
    @JsonIncludeProperties(value = {"username", "code"})
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private UserEntity user;
}
