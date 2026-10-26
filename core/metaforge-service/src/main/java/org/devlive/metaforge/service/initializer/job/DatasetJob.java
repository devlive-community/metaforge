package org.devlive.metaforge.service.initializer.job;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.service.service.DataSetService;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public class DatasetJob
        extends QuartzJobBean
{
    private DataSetService service;

    @Autowired
    public void setService(DataSetService service)
    {
        this.service = service;
    }

    @Override
    protected void executeInternal(JobExecutionContext context)
    {
        Object idValue = context.getJobDetail().getJobDataMap().get("id");
        if (idValue == null) {
            log.warn("Job [ {} ] skipped: missing 'id' in JobDataMap", context.getJobDetail().getKey());
            return;
        }
        String code = idValue.toString();
        log.info("Job [ {} ] run time [ {} ]", code, context.getFireTime().getTime());
        this.service.syncData(code);
    }
}
