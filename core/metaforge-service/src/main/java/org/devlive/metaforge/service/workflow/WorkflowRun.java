package org.devlive.metaforge.service.workflow;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import org.devlive.metaforge.executor.common.RunState;
import org.devlive.metaforge.executor.configure.ExecutorMetrics;

import java.util.List;
import java.util.Map;

@Data
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class WorkflowRun
{
    private String id;
    private String code;
    private String name;
    private String executor;
    private volatile RunState state;
    private long startedAt;
    private Long finishedAt;
    private long elapsed;
    private String message;
    private Integer count;
    private volatile ExecutorMetrics metrics;
    private List<Map<String, String>> nodes;
    private List<Map<String, Object>> preview;
}
