package org.devlive.metaforge.service.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.devlive.metaforge.service.enums.MessageType;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(value = AuditingEntityListener.class)
@Table(name = "metaforge_message")
@SuppressFBWarnings(value = {"EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC", "EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification = "I prefer to suppress these FindBugs warnings")
public class MessageEntity
        extends BaseEntity
{
    @Column(name = "content")
    private String content;

    @Column(name = "model")
    private String model;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private MessageType type;

    @Column(name = "prompt_tokens")
    private long promptTokens;

    @Column(name = "completion_tokens")
    private long completionTokens;

    @Column(name = "total_tokens")
    private long totalTokens;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinTable(name = "metaforge_message_user_relation",
            joinColumns = @JoinColumn(name = "message_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"))
    @JsonIgnoreProperties(value = {"roles", "thirdConfigure"})
    private UserEntity user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinTable(name = "metaforge_message_chat_relation",
            joinColumns = @JoinColumn(name = "message_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "chat_id", referencedColumnName = "id"))
    @JsonIgnoreProperties(value = {"user"})
    private ChatEntity chat;
}
