package org.devlive.metaforge.service.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.devlive.metaforge.service.enums.ColumnMode;
import org.devlive.metaforge.service.enums.ColumnType;
import org.devlive.metaforge.service.persistence.Reference;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@SuperBuilder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table("metaforge_dataset_column")
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC"})
public class DataSetColumnEntity
        extends BaseEntity
{
    @Column("type")
    private ColumnType type;

    @Column("column_comment")
    private String comment;

    @Column("original")
    private String original;

    @Column("default_value")
    private String defaultValue;

    @Column("expression")
    private String formula;

    @Column("position")
    private int position;

    @Column("is_nullable")
    private boolean nullable;

    @Column("length")
    private int length;

    @Column("is_order_by_key")
    private boolean orderByKey;

    @Column("is_partition_key")
    private boolean partitionKey;

    @Column("is_primary_key")
    private boolean primaryKey;

    @Column("is_sampling_key")
    private boolean samplingKey;

    @Column("column_mode")
    private ColumnMode mode;

    @Column("alias_name")
    private String aliasName;

    @Column("is_custom_column")
    private boolean customColumn;

    @Column("is_virtual_column")
    private boolean virtualColumn;

    @Column("dataset_id")
    private Long datasetId;

    @Transient
    @Reference("datasetId")
    private DataSetEntity dataset;
}
