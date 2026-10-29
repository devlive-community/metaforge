package org.devlive.metaforge.service.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.devlive.metaforge.common.view.EntityView;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;

import java.util.Date;

@Data
@SuperBuilder(toBuilder = true)
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE"},
        justification = "I prefer to suppress these FindBugs warnings")
public class BaseEntity
{
    @Id
    @Column("id")
    @JsonView(value = {EntityView.AdminView.class})
    private Long id;

    @Column("name")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String name;

    @Column("code")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String code;

    @Column("active")
    @JsonView(value = {EntityView.AdminView.class})
    private boolean active = true;

    @Column("create_time")
    @CreatedDate
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Date createTime;

    @Setter @Column("update_time")
    @LastModifiedDate
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Date updateTime;

    public void setCode(String code)
    {
        if (this.code == null) {
            this.code = code;
        }
    }
}
