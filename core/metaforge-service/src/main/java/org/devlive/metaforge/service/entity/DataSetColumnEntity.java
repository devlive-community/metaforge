package org.devlive.metaforge.service.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.devlive.metaforge.service.enums.ColumnMode;
import org.devlive.metaforge.service.enums.ColumnType;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Data
@SuperBuilder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "metaforge_dataset_column")
@EntityListeners(AuditingEntityListener.class)
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC"})
public class DataSetColumnEntity
        extends BaseEntity
{
    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private ColumnType type;

    @Column(name = "comment")
    private String comment;

    @Column(name = "original")
    private String original;

    @Column(name = "default_value")
    private String defaultValue;

    @Column(name = "position")
    private int position;

    @Column(name = "is_nullable")
    private boolean nullable;

    @Column(name = "length")
    private int length;

    @Column(name = "is_order_by_key")
    private boolean orderByKey;

    @Column(name = "is_partition_key")
    private boolean partitionKey;

    @Column(name = "is_primary_key")
    private boolean primaryKey;

    @Column(name = "is_sampling_key")
    private boolean samplingKey;

    @Column(name = "column_mode")
    @Enumerated(EnumType.STRING)
    private ColumnMode mode;

    @Column(name = "alias_name")
    private String aliasName;

    @Column(name = "is_custom_column")
    private boolean customColumn;

    @Column(name = "is_virtual_column")
    private boolean virtualColumn;

    @ManyToOne
    @JoinTable(name = "metaforge_dataset_column_relation",
            joinColumns = @JoinColumn(name = "column_id"),
            inverseJoinColumns = @JoinColumn(name = "dataset_id"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private DataSetEntity dataset;
}
