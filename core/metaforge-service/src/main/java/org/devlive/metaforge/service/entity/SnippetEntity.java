package org.devlive.metaforge.service.entity;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.devlive.metaforge.common.view.EntityView;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "metaforge_snippet")
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC"},
        justification = "I prefer to suppress these FindBugs warnings")
@EntityListeners(AuditingEntityListener.class)
public class SnippetEntity
        extends BaseEntity
{
    @Column(name = "description")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String description;

    @Column(name = "context")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String context;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIncludeProperties(value = {"id", "username", "avatarConfigure"})
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private UserEntity user;
}
