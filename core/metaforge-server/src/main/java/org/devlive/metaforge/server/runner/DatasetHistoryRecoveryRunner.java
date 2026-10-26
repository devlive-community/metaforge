package org.devlive.metaforge.server.runner;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.devlive.metaforge.executor.common.RunState;
import org.devlive.metaforge.service.entity.DatasetHistoryEntity;
import org.devlive.metaforge.service.initializer.InstallationState;
import org.devlive.metaforge.service.repository.DatasetHistoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 启动时把上次服务异常退出留下的"未完结"同步历史标记为 INTERRUPTED。
 * 仅处理 RUNNING / CREATED / STOPPING 这些非终态。
 * 在 SchedulerRunner 之前执行（@Order 较小），避免被即将启动的定时任务"运行中"覆盖。
 *
 * On startup, mark sync-history rows left in transient states by a prior crash as INTERRUPTED.
 */
@Slf4j
@Service
@Order(10)
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public class DatasetHistoryRecoveryRunner
        implements CommandLineRunner
{
    private static final List<RunState> TRANSIENT_STATES = Arrays.asList(
            RunState.RUNNING,
            RunState.CREATED,
            RunState.STOPPING
    );

    private final DatasetHistoryRepository historyRepository;
    private final InstallationState installationState;

    public DatasetHistoryRecoveryRunner(DatasetHistoryRepository historyRepository, InstallationState installationState)
    {
        this.historyRepository = historyRepository;
        this.installationState = installationState;
    }

    @Override
    public void run(String... args)
    {
        if (!installationState.isInstalled()) {
            log.info("Dataset history recovery skipped: instance is not installed");
            return;
        }
        try {
            List<DatasetHistoryEntity> stale = historyRepository.findAllByStateIn(TRANSIENT_STATES);
            if (stale.isEmpty()) {
                log.info("Dataset history recovery: no stale records.");
                return;
            }
            String now = DateFormatUtils.format(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss");
            String message = String.format("Marked as INTERRUPTED by server restart at %s (previous state was transient).", now);
            Date updateTime = new Date();
            for (DatasetHistoryEntity history : stale) {
                RunState previous = history.getState();
                history.setState(RunState.INTERRUPTED);
                history.setMessage(message);
                history.setUpdateTime(updateTime);
                log.info("Recovering sync history [ {} ] previousState={} -> INTERRUPTED", history.getId(), previous);
            }
            historyRepository.saveAll(stale);
            log.info("Dataset history recovery: {} stale record(s) marked INTERRUPTED.", stale.size());
        }
        catch (Exception ex) {
            log.error("Dataset history recovery failed", ex);
        }
    }
}
