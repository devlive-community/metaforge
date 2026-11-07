package org.devlive.metaforge.executor.common

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.executor.common.RunState.CREATED
import org.devlive.metaforge.executor.common.RunState.FAILURE
import org.devlive.metaforge.executor.common.RunState.INTERRUPTED
import org.devlive.metaforge.executor.common.RunState.PAUSED
import org.devlive.metaforge.executor.common.RunState.PAUSING
import org.devlive.metaforge.executor.common.RunState.QUEUE
import org.devlive.metaforge.executor.common.RunState.RETRYING
import org.devlive.metaforge.executor.common.RunState.RUNNING
import org.devlive.metaforge.executor.common.RunState.STOPPED
import org.devlive.metaforge.executor.common.RunState.STOPPING
import org.devlive.metaforge.executor.common.RunState.SUCCESS
import org.devlive.metaforge.executor.common.RunState.TIMEOUT

/**
 * 同步任务的合法状态转换。
 *
 * - 终态：SUCCESS / FAILURE / TIMEOUT / STOPPED / INTERRUPTED；除 SUCCESS 外都可手动重试回到 QUEUE
 * - STOPPING / PAUSING 期间任务可能已自然结束，所以允许直接落到 SUCCESS / FAILURE / TIMEOUT
 * - RUNNING -> STOPPED / PAUSED 允许：取消或暂停可能先于 STOPPING / PAUSING 落库就完成
 * - 任何非终态在服务重启后都会被标记为 INTERRUPTED
 *
 * Legal state transitions of a sync task. Every non-terminal state may become INTERRUPTED on restart.
 */
@SuppressFBWarnings(value = ["BC_BAD_CAST_TO_ABSTRACT_COLLECTION"])
object RunStateMachine
{
    private val transitions: Map<RunState, Set<RunState>> = mapOf(
        CREATED to setOf(QUEUE, STOPPED, FAILURE, INTERRUPTED),
        QUEUE to setOf(RUNNING, STOPPED, FAILURE, INTERRUPTED),
        RUNNING to setOf(PAUSING, PAUSED, STOPPING, RETRYING, SUCCESS, FAILURE, TIMEOUT, STOPPED, INTERRUPTED),
        PAUSING to setOf(PAUSED, STOPPING, SUCCESS, FAILURE, TIMEOUT, STOPPED, INTERRUPTED),
        PAUSED to setOf(QUEUE, STOPPED, INTERRUPTED),
        STOPPING to setOf(STOPPED, SUCCESS, FAILURE, TIMEOUT, INTERRUPTED),
        RETRYING to setOf(RUNNING, STOPPED, FAILURE, INTERRUPTED),
        SUCCESS to emptySet(),
        FAILURE to setOf(QUEUE),
        TIMEOUT to setOf(QUEUE),
        STOPPED to setOf(QUEUE),
        INTERRUPTED to setOf(QUEUE)
    )

    private val terminal: Set<RunState> = setOf(SUCCESS, FAILURE, TIMEOUT, STOPPED, INTERRUPTED)

    @JvmStatic
    fun canTransition(from: RunState, to: RunState): Boolean = transitions[from]?.contains(to) == true

    @JvmStatic
    fun isTerminal(state: RunState): Boolean = state in terminal

    /** 所有非终态，供重启恢复等场景使用 / Every non-terminal state */
    @JvmStatic
    fun activeStates(): List<RunState> = RunState.values().filterNot { it in terminal }
}
