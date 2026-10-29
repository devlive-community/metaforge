package org.devlive.metaforge.service.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.devlive.metaforge.service.enums.ScheduledType;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@ToString
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table("metaforge_scheduled")
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC"})
public class ScheduledEntity
        extends BaseEntity
{
    @Column("description")
    private String description;

    @Column("expression")
    private String expression;

    @Column("is_system")
    private boolean isSystem;

    @Column("type")
    private ScheduledType type;
}
