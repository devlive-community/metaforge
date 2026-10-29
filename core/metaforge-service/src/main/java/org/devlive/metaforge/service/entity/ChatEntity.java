package org.devlive.metaforge.service.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.devlive.metaforge.service.persistence.Reference;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@SuperBuilder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table("metaforge_chat")
@SuppressFBWarnings(value = {"EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC", "EI_EXPOSE_REP2", "EI_EXPOSE_REP"},
        justification = "I prefer to suppress these FindBugs warnings")
public class ChatEntity
        extends BaseEntity
{
    @Column("avatar")
    private String avatar;

    @Column("description")
    private String description;

    @Column("user_id")
    private Long userId;

    @Transient
    @Reference("userId")
    @JsonIgnoreProperties(value = {"roles", "thirdConfigure", "sources"})
    private UserEntity user;
}
