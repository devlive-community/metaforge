package org.devlive.metaforge.service.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Date;

/** A user's account at a third-party sign-in provider, found again by the provider's subject id. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("metaforge_user_oauth")
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class UserOAuthEntity
{
    @Id
    private Long id;

    @Column("user_id")
    private Long userId;

    // github, google or oidc
    @Column("provider")
    private String provider;

    @Column("subject")
    private String subject;

    @Column("create_time")
    private Date createTime;
}
