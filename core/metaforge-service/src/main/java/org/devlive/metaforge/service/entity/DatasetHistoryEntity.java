package org.devlive.metaforge.service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @Column("rows_read")
    private Long rowsRead;

    @Column("rows_written")
    private Long rowsWritten;

    @Column("rows_skipped")
    private Long rowsSkipped;

    @Column("rows_failed")
    private Long rowsFailed;

    @Column("bytes_read")
    private Long bytesRead;

    @Column("bytes_written")
    private Long bytesWritten;

    /** 第几次尝试，从 1 开始 / attempt number, starting at 1 */
    @Column("attempt")
    private Integer attempt;

    /** 最近一次（结束时为最终）指标快照 JSON / latest (final once finished) metrics snapshot as JSON */
    @Column("metrics")
    private String metrics;

    /** 状态转换记录 JSON 数组 / state transitions as a JSON array */
    @Column("transitions")
    private String transitions;

    /** 未完成时用于精确续传的断点（内容由执行器定义）/ checkpoint to resume an unfinished run exactly, owned by the executor */
    @JsonIgnore
    @Column("checkpoint")
    private String checkpoint;

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
