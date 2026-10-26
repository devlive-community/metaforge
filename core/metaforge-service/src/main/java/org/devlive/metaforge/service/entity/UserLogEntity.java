package org.devlive.metaforge.service.entity;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.devlive.metaforge.common.enums.State;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import java.sql.Timestamp;

@Entity
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "metaforge_user_log")
@SuppressFBWarnings(value = {"EI_EXPOSE_REP"},
        justification = "I prefer to suppress these FindBugs warnings")
public class UserLogEntity
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device")
    private String device;

    @Column(name = "client")
    private String client;

    @Column(name = "ip")
    private String ip;

    @Column(name = "ua")
    private String ua;

    @Column(name = "state", unique = true)
    @Enumerated(EnumType.STRING)
    private State state;

    @Column(name = "message", unique = true)
    private String message;

    @Column(name = "create_time", columnDefinition = "datetime(5) default CURRENT_TIMESTAMP()")
    private Timestamp createTime;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIncludeProperties(value = {"id", "username"})
    private UserEntity user;
}
