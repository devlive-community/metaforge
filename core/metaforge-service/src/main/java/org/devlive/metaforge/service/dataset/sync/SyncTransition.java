package org.devlive.metaforge.service.dataset.sync;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.devlive.metaforge.executor.common.RunState;

import java.util.Date;

/**
 * 一次状态转换记录：从哪个状态到哪个状态、发生时间（毫秒时间戳）与原因。
 * One recorded state change of a sync task.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
public class SyncTransition
{
    private RunState from;
    private RunState to;
    private long at;
    private String message;

    /** The transition time as the server formats every date; {@link #at} stays for ordering. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    public Date getTime()
    {
        return at > 0 ? new Date(at) : null;
    }
}
