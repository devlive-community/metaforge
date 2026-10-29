package org.devlive.metaforge.service.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.devlive.metaforge.executor.common.RunState;
import org.devlive.metaforge.service.enums.QueryMode;
import org.devlive.metaforge.service.persistence.Reference;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("metaforge_dataset_history")
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC"})
public class DatasetHistoryEntity
        extends BaseEntity
{
    @Column("query")
    private String query;

    @Column("message")
    private String message;

    @Column("elapsed")
    private Long elapsed;

    @Column("count")
    private int count;

    @Column("total_count")
    private Long totalCount;

    @Column("processed_count")
    private Long processedCount;

    @Column("progress")
    private BigDecimal progress;

    @Column("task_name")
    private String taskName;

    @Column("work_home")
    private String workHome;

    /** 本次同步实际生效的 executor 配置 JSON（DB 默认 + 本次临时覆盖后的合并结果） */
    @Column("executor_configure")
    private String executorConfigure;

    @Column("query_mode")
    private QueryMode mode;

    @Column("state")
    private RunState state;

    @Column("dataset_id")
    private Long datasetId;

    @Transient
    @Reference("datasetId")
    private DataSetEntity dataset;
}
