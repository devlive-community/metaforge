package org.devlive.metaforge.executor.configure

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.executor.common.RunState

@SuppressFBWarnings(value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2"])
data class ExecutorResponse(var timeout: Boolean = false,
                            var successful: Boolean = false,
                            var state: RunState? = null,
                            var message: String? = null,
                            var count: Int = 0,
                            /** 结束时的最终指标；执行器不上报指标时为 null / final metrics, null if not reported */
                            var metrics: ExecutorMetrics? = null) {
    constructor(timeout: Boolean = false,
                successful: Boolean = false,
                state: RunState? = null,
                message: String? = null) : this(timeout, successful, state, message, 0)
}
