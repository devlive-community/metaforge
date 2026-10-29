package org.devlive.metaforge.service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.common.utils.JsonUtils;
import org.devlive.metaforge.executor.common.RunState;
import org.devlive.metaforge.service.persistence.Reference;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Properties;

@Getter
@Setter
@ToString
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table("metaforge_scheduled_history")
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EQ_DOESNT_OVERRIDE_EQUALS"})
public class ScheduledHistoryEntity
        extends BaseEntity
{
    // JSON object; exposed as Properties through getInfo/setInfo.
    @Column("info")
    @JsonIgnore
    private String infoValue;

    public Properties getInfo()
    {
        return StringUtils.isEmpty(infoValue) ? new Properties() : JsonUtils.toProperties(infoValue);
    }

    public void setInfo(Properties info)
    {
        this.infoValue = info == null ? null : JsonUtils.toJSON(info);
    }

    @Column("state")
    private RunState state;

    @Column("message")
    private String message;

    @Column("scheduled_id")
    private Long scheduledId;

    @Transient
    @Reference("scheduledId")
    private ScheduledEntity scheduled;

    @Column("source_id")
    private Long sourceId;

    @Transient
    @Reference("sourceId")
    private SourceEntity source;
}
