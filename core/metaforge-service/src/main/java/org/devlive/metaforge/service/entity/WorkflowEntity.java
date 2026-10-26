package org.devlive.metaforge.service.entity;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.devlive.metaforge.common.view.EntityView;
import org.devlive.metaforge.executor.common.RunState;
import org.devlive.metaforge.service.converter.ConfigurationConverter;
import org.devlive.metaforge.service.itransient.configuration.Configuration;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Data
@ToString
@NoArgsConstructor
@Entity
@Table(name = "metaforge_workflow")
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC"})
public class WorkflowEntity
        extends BaseEntity
{
    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private RunState state;

    @Column(name = "message")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String message;

    @Column(name = "work")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String work;

    @Column(name = "elapsed")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Long elapsed;

    @Column(name = "executor")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String executor;

    @Column(name = "configure")
    @Convert(converter = ConfigurationConverter.class)
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Configuration configure;

    @ManyToOne
    @JoinColumn(name = "j_from_id")
    @JsonIncludeProperties(value = {"code", "name", "type"})
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private SourceEntity from;

    @ManyToOne
    @JoinColumn(name = "j_to_id")
    @JsonIncludeProperties(value = {"code", "name", "type"})
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private SourceEntity to;

    @ManyToOne
    @JoinColumn(name = "j_user_id")
    @JsonIncludeProperties(value = {"username", "code"})
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private UserEntity user;
}
