package org.devlive.metaforge.service.workflow;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import org.devlive.metaforge.executor.common.RunState;
import org.devlive.metaforge.executor.configure.ExecutorMetrics;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
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

    /** Start time as the server formats every date (yyyy-MM-dd HH:mm:ss); the epoch value stays for computation. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    public Date getStartTime()
    {
        return startedAt > 0 ? new Date(startedAt) : null;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    public Date getFinishTime()
    {
        return finishedAt == null ? null : new Date(finishedAt);
    }
}
