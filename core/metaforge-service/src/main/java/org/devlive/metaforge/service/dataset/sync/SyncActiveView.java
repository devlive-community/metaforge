package org.devlive.metaforge.service.dataset.sync;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.devlive.metaforge.executor.common.RunState;

/**
 * 列表页展示的一次未结束同步：状态、进度与写入速率。progress 未知时为 -1。
 * One unfinished sync as the list page shows it; progress is -1 when unknown.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncActiveView
{
    private String datasetCode;
    private Long historyId;
    private RunState state;
    private double progress;
    private long rowsWritten;
    private double writeRowsPerSecond;
}
