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
    /** Additive property: keep existing plugin constructor/copy signatures compatible. */
    var preview: List<Map<String, Any?>>? = null

    /**
     * 未完成时可用于续传的断点；为空表示不能精确续传（再次运行可能重复写入）。附加属性。
     * Checkpoint to resume an unfinished run from; null when it cannot resume exactly. Additive property.
     */
    var checkpoint: String? = null

    /**
     * 失败是暂时性的（如断连），且再次运行不会重复写入。附加属性。
     * The failure was transient (a lost connection, say) and running again writes nothing twice. Additive property.
     */
    var retryable: Boolean = false

    constructor(timeout: Boolean = false,
                successful: Boolean = false,
                state: RunState? = null,
                message: String? = null) : this(timeout, successful, state, message, 0)
}
