package org.devlive.metaforge.service.entity;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.devlive.metaforge.common.enums.State;
import org.devlive.metaforge.service.persistence.Reference;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.sql.Timestamp;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table("metaforge_user_log")
@SuppressFBWarnings(value = {"EI_EXPOSE_REP"},
        justification = "I prefer to suppress these FindBugs warnings")
public class UserLogEntity
{
    @Id
    private Long id;

    @Column("device")
    private String device;

    @Column("client")
    private String client;

    @Column("ip")
    private String ip;

    @Column("ua")
    private String ua;

    @Column("state")
    private State state;

    @Column("message")
    private String message;

    @Column("create_time")
    private Timestamp createTime;

    @Column("user_id")
    private Long userId;

    @Transient
    @Reference("userId")
    @JsonIncludeProperties(value = {"id", "username"})
    private UserEntity user;
}
