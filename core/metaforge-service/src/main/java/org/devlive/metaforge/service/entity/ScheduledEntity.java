package org.devlive.metaforge.service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.devlive.metaforge.service.enums.ScheduledType;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import java.util.List;

@Data
@ToString
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "metaforge_scheduled")
@EntityListeners(AuditingEntityListener.class)
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC"})
public class ScheduledEntity
        extends BaseEntity
{
    @Column(name = "description")
    private String description;

    @Column(name = "expression")
    private String expression;

    @Column(name = "is_system")
    private boolean isSystem;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private ScheduledType type;

    @OneToMany(mappedBy = "scheduled", cascade = CascadeType.REMOVE)
    @JsonIgnore
    private List<ScheduledHistoryEntity> historys;
}
