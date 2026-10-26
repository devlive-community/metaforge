package org.devlive.metaforge.service.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.devlive.metaforge.executor.common.RunState;
import org.devlive.metaforge.service.enums.QueryMode;
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

import java.math.BigDecimal;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "metaforge_dataset_history")
@EntityListeners(AuditingEntityListener.class)
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC"})
public class DatasetHistoryEntity
        extends BaseEntity
{
    @Column(name = "query")
    private String query;

    @Column(name = "message", unique = true)
    private String message;

    @Column(name = "elapsed")
    private Long elapsed;

    @Column(name = "count")
    private int count;

    @Column(name = "total_count")
    private Long totalCount;

    @Column(name = "processed_count")
    private Long processedCount;

    @Column(name = "progress")
    private BigDecimal progress;

    @Column(name = "task_name")
    private String taskName;

    @Column(name = "work_home")
    private String workHome;

    /** 本次同步实际生效的 executor 配置 JSON（DB 默认 + 本次临时覆盖后的合并结果） */
    @Column(name = "executor_configure", columnDefinition = "TEXT")
    private String executorConfigure;

    @Column(name = "query_mode")
    @Enumerated(EnumType.STRING)
    private QueryMode mode;

    @Column(name = "state", unique = true)
    @Enumerated(EnumType.STRING)
    private RunState state;

    @ManyToOne
    @JoinTable(name = "metaforge_dataset_history_relation",
            joinColumns = @JoinColumn(name = "dataset_history_id"),
            inverseJoinColumns = @JoinColumn(name = "dataset_id"))
    private DataSetEntity dataset;
}
