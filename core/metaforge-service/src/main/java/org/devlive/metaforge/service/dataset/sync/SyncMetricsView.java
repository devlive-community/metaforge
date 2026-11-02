package org.devlive.metaforge.service.dataset.sync;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.devlive.metaforge.executor.common.RunState;
import org.devlive.metaforge.executor.configure.ExecutorMetrics;

import java.util.List;

/**
 * 一次同步的实时视图：状态、尝试次数、最新指标与状态转换记录。live 表示数据来自内存中的运行任务，
 * 否则来自落库的最终结果。
 *
 * Live view of one sync run; {@code live} tells a running task from a persisted, finished one.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class SyncMetricsView
{
    private Long historyId;
    private RunState state;
    private int attempt;
    private boolean live;
    private String message;
    private ExecutorMetrics metrics;
    private List<SyncTransition> transitions;
}
