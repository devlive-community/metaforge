package org.devlive.metaforge.service.dataset.sync;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.devlive.metaforge.executor.common.RunState;

/**
 * 一次状态转换记录：从哪个状态到哪个状态、发生时间（毫秒时间戳）与原因。
 * One recorded state change of a sync task.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncTransition
{
    private RunState from;
    private RunState to;
    private long at;
    private String message;
}
