package org.devlive.metaforge.service.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.devlive.metaforge.executor.common.RunState;
import org.devlive.metaforge.service.converter.PropertiesConverter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import java.util.Properties;

@Getter
@Setter
@ToString
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "metaforge_scheduled_history")
@EntityListeners(AuditingEntityListener.class)
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EQ_DOESNT_OVERRIDE_EQUALS"})
public class ScheduledHistoryEntity
        extends BaseEntity
{
    @Column(name = "info")
    @Convert(converter = PropertiesConverter.class)
    private Properties info;

    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    private RunState state;

    @Column(name = "message")
    private String message;

    @ManyToOne
    @JoinTable(name = "metaforge_scheduled_history_relation",
            joinColumns = @JoinColumn(name = "scheduled_id"),
            inverseJoinColumns = @JoinColumn(name = "history_id"))
    private ScheduledEntity scheduled;

    @ManyToOne
    @JoinTable(name = "metaforge_scheduled_history_source_relation",
            joinColumns = @JoinColumn(name = "scheduled_history_id"),
            inverseJoinColumns = @JoinColumn(name = "source_id"))
    private SourceEntity source;
}
