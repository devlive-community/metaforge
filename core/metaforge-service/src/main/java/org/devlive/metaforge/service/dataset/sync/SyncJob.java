package org.devlive.metaforge.service.dataset.sync;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.executor.common.RunState;
import org.devlive.metaforge.executor.configure.ExecutorMetrics;
import org.devlive.metaforge.service.entity.DatasetHistoryEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 一次正在排队或运行的数据集同步。它独占对应的 history 实体：所有修改与落库都经过本对象并在其上加锁，
 * 避免状态转换、进度刷新与同步主体互相覆盖。
 *
 * One queued or running dataset sync. It owns its history row: every change and save goes through
 * this object under its lock, so state changes, progress updates and the sync body never overwrite
 * each other.
 */
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class SyncJob
{
    private final SyncJobManager manager;
    private final DatasetHistoryEntity history;
    private final Long datasetId;
    private final List<SyncTransition> transitions = new ArrayList<>();
    private final Consumer<SyncJob> body;

    private volatile ExecutorMetrics metrics;
    private volatile long startedAt;
    private long lastPersistAt;

    SyncJob(SyncJobManager manager, DatasetHistoryEntity history, Consumer<SyncJob> body)
    {
        this.manager = manager;
        this.history = history;
        this.body = body;
        this.datasetId = history.getDatasetId();
    }

    public Long getHistoryId()
    {
        return history.getId();
    }

    public Long getDatasetId()
    {
        return datasetId;
    }

    public synchronized RunState getState()
    {
        return history.getState();
    }

    /** 续传用的断点，没有则为 null / the checkpoint to resume from, null without one */
    public synchronized String getCheckpoint()
    {
        return history.getCheckpoint();
    }

    /** 首次运行前为 null，之后沿用同一任务名 / null before the first run; later runs keep the task name */
    public synchronized String getTaskName()
    {
        return history.getTaskName();
    }

    /** 首次运行时确定的配置 JSON，续传沿用 / the configuration JSON fixed by the first run, kept by later runs */
    public synchronized String getExecutorConfigure()
    {
        return history.getExecutorConfigure();
    }

    Consumer<SyncJob> body()
    {
        return body;
    }

    public ExecutorMetrics getMetrics()
    {
        return metrics;
    }

    /** 进入 RUNNING 的时间戳，尚未运行为 0 / when the task started running, 0 before that */
    public long getStartedAt()
    {
        return startedAt;
    }

    public synchronized List<SyncTransition> getTransitions()
    {
        return new ArrayList<>(transitions);
    }

    /** 在锁内修改 history 并落库 / change the history row under the lock and save it */
    public synchronized void update(Consumer<DatasetHistoryEntity> change)
    {
        change.accept(history);
        manager.save(history);
    }

    /** 状态转换，不合法时拒绝并返回 false / move to a new state, false when the move is not legal */
    public boolean transition(RunState to, String message)
    {
        return manager.transition(this, to, message);
    }

    synchronized DatasetHistoryEntity history()
    {
        return history;
    }

    synchronized void recordTransition(SyncTransition transition)
    {
        transitions.add(transition);
        if (transition.getTo() == RunState.RUNNING && startedAt == 0) {
            startedAt = transition.getAt();
        }
    }

    void setMetrics(ExecutorMetrics metrics)
    {
        this.metrics = metrics;
    }

    synchronized boolean persistDue(long now, long interval)
    {
        if (now - lastPersistAt < interval) {
            return false;
        }
        lastPersistAt = now;
        return true;
    }
}
