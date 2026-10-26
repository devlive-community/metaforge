package org.devlive.metaforge.service.wrapper;

import com.fasterxml.jackson.annotation.JsonView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import org.devlive.metaforge.common.view.EntityView;
import org.devlive.metaforge.spi.model.Time;

@Data
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class TimeWrapper
{
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private long start;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private long end;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private long elapsed;

    public static TimeWrapper from(Time time)
    {
        TimeWrapper wrapper = new TimeWrapper();
        wrapper.setStart(time.getStart());
        wrapper.setEnd(time.getEnd());
        wrapper.setElapsed(time.getElapsed());
        return wrapper;
    }
}
