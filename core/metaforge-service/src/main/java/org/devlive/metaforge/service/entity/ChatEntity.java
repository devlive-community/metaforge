package org.devlive.metaforge.service.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Data
@SuperBuilder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(value = AuditingEntityListener.class)
@Table(name = "metaforge_chat")
@SuppressFBWarnings(value = {"EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC", "EI_EXPOSE_REP2", "EI_EXPOSE_REP"},
        justification = "I prefer to suppress these FindBugs warnings")
public class ChatEntity
        extends BaseEntity
{
    @Column(name = "avatar")
    private String avatar;

    @Column(name = "description")
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinTable(name = "metaforge_chat_user_relation",
            joinColumns = @JoinColumn(name = "chat_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"))
    @JsonIgnoreProperties(value = {"roles", "thirdConfigure", "sources"})
    private UserEntity user;
}
