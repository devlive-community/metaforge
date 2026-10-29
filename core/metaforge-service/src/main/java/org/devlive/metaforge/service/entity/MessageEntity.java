package org.devlive.metaforge.service.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.devlive.metaforge.service.enums.MessageType;
import org.devlive.metaforge.service.persistence.Reference;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table("metaforge_message")
@SuppressFBWarnings(value = {"EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC", "EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification = "I prefer to suppress these FindBugs warnings")
public class MessageEntity
        extends BaseEntity
{
    @Column("content")
    private String content;

    @Column("model")
    private String model;

    @Column("type")
    private MessageType type;

    @Column("prompt_tokens")
    private long promptTokens;

    @Column("completion_tokens")
    private long completionTokens;

    @Column("total_tokens")
    private long totalTokens;

    @Column("user_id")
    private Long userId;

    @Transient
    @Reference("userId")
    @JsonIgnoreProperties(value = {"roles", "thirdConfigure"})
    private UserEntity user;

    @Column("chat_id")
    private Long chatId;

    @Transient
    @Reference("chatId")
    @JsonIgnoreProperties(value = {"user"})
    private ChatEntity chat;
}
