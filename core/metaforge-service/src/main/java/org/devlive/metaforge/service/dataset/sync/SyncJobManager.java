package org.devlive.metaforge.service.dataset.sync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.executor.common.RunState;
import org.devlive.metaforge.executor.common.RunStateMachine;
import org.devlive.metaforge.executor.configure.ExecutorMetrics;
import org.devlive.metaforge.executor.configure.StageMetrics;
import org.devlive.metaforge.service.configure.DatasetTargetSchema;
import org.devlive.metaforge.service.entity.DatasetHistoryEntity;
import org.devlive.metaforge.service.repository.DatasetHistoryRepository;
import org.devlive.metaforge.service.service.RuntimeConfigureService;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 数据集同步的统一调度：
 * - 全局 FIFO 队列，最多同时运行 {@code maxConcurrentSyncs} 个（系统设置 DATASET/Default，默认 4）
 * - 同一数据集同一时刻只允许一个排队或运行中的同步
 * - 所有状态转换经 {@link RunStateMachine} 校验，并记录到 history.transitions
 * - 指标按时间节流落库，状态变化与结束时立即落库
 *
 * Schedules dataset syncs: one global FIFO queue capped at {@code maxConcurrentSyncs}, at most one
 * queued or running sync per dataset, validated state transitions, and throttled metric persistence.
 */
@Slf4j
@Component
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public class SyncJobManager
{
    public static final String MAX_CONCURRENT_SYNCS = "maxConcurrentSyncs";
    static final int DEFAULT_MAX_CONCURRENT_SYNCS = 4;
    /** 运行中指标的落库间隔 / how often running metrics are saved */
    static final long PERSIST_INTERVAL_MS = 2000;

    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final TypeReference<List<SyncTransition>> TRANSITIONS = new TypeReference<>() {};

    private final DatasetHistoryRepository historyRepository;
    private final RuntimeConfigureService runtimeConfigureService;
    private final ThreadPoolExecutor pool;
    private final Map<Long, SyncJob> jobs = new ConcurrentHashMap<>();
    private final Map<Long, Long> activeByDataset = new ConcurrentHashMap<>();

    public SyncJobManager(DatasetHistoryRepository historyRepository, RuntimeConfigureService runtimeConfigureService)
    {
        this.historyRepository = historyRepository;
        this.runtimeConfigureService = runtimeConfigureService;
        AtomicInteger threads = new AtomicInteger();
        this.pool = new ThreadPoolExecutor(DEFAULT_MAX_CONCURRENT_SYNCS, DEFAULT_MAX_CONCURRENT_SYNCS, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(), runnable -> {
                    Thread thread = new Thread(runnable, "dataset-sync-" + threads.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                });
        this.pool.allowCoreThreadTimeOut(true);
    }

    @PreDestroy
    public void shutdown()
    {
        pool.shutdownNow();
    }

    /**
     * 提交一次同步。数据集已有排队或运行中的同步时返回 empty，且不会调用 historyFactory。
     * historyFactory 负责创建并保存 CREATED 状态的 history；body 在工作线程中运行同步主体，
     * 需要自行把任务推进到终态，未推进则按 FAILURE 收尾。
     *
     * Submit a sync. Returns empty (without calling historyFactory) when the dataset already has a
     * queued or running sync. The body must drive the job to a terminal state; otherwise it ends as FAILURE.
     */
    public Optional<SyncJob> submit(Long datasetId, Supplier<DatasetHistoryEntity> historyFactory, Consumer<SyncJob> body)
    {
        if (activeByDataset.putIfAbsent(datasetId, -1L) != null) {
            return Optional.empty();
        }
        SyncJob job;
        try {
            DatasetHistoryEntity history = historyFactory.get();
            job = new SyncJob(this, history, body);
            jobs.put(job.getHistoryId(), job);
            activeByDataset.put(datasetId, job.getHistoryId());
            job.recordTransition(new SyncTransition(null, history.getState(), System.currentTimeMillis(), null));
        }
        catch (RuntimeException e) {
            activeByDataset.remove(datasetId, -1L);
            throw e;
        }
        transition(job, RunState.QUEUE, null);
        resizePool();
        pool.execute(() -> run(job));
        return Optional.of(job);
    }

    public Optional<SyncJob> find(Long historyId)
    {
        return Optional.ofNullable(historyId == null ? null : jobs.get(historyId));
    }

    /**
     * 继续一个已暂停的同步：回到队列，轮到时从断点续传。
     * Resume a paused sync: back into the queue, continuing from its checkpoint when its turn comes.
     */
    public boolean resume(SyncJob job)
    {
        if (job.getState() != RunState.PAUSED || !transition(job, RunState.QUEUE, null)) {
            return false;
        }
        resizePool();
        pool.execute(() -> run(job));
        return true;
    }

    /**
     * 让已结束的同步再次运行：同一条历史，尝试次数加一，有断点时从断点续传。数据集已有未结束的同步时返回 empty。
     * Run a finished sync again on the same history row, one attempt up, from its checkpoint if it has one.
     * Returns empty while the dataset has an unfinished sync.
     */
    public Optional<SyncJob> retry(DatasetHistoryEntity history, Consumer<SyncJob> body)
    {
        Long datasetId = history.getDatasetId();
        if (activeByDataset.putIfAbsent(datasetId, history.getId()) != null) {
            return Optional.empty();
        }
        SyncJob job = new SyncJob(this, history, body);
        readTransitions(history.getTransitions()).forEach(job::recordTransition);
        jobs.put(history.getId(), job);
        job.update(row -> {
            row.setAttempt(attemptOf(row) + 1);
            row.setMessage(null);
        });
        transition(job, RunState.QUEUE, null);
        resizePool();
        pool.execute(() -> run(job));
        return Optional.of(job);
    }

    /**
     * 已结束的同步能否再次运行而不重复写入：有精确断点，或确认没有写入过任何行。异常中断时已写行数未必是最新的，
     * 只认断点。
     * A finished sync may run again only with an exact checkpoint or when it certainly wrote nothing. After
     * a crash the saved row count may be stale, so only a checkpoint counts.
     */
    public static boolean canRetry(DatasetHistoryEntity history)
    {
        RunState state = history.getState();
        if (state == null || !RunStateMachine.canTransition(state, RunState.QUEUE) || !RunStateMachine.isTerminal(state)) {
            return false;
        }
        if (history.getCheckpoint() != null) {
            return true;
        }
        return state != RunState.INTERRUPTED && (history.getRowsWritten() == null || history.getRowsWritten() == 0);
    }

    /** 所有未结束的同步，包括排队和暂停的 / every unfinished sync, queued and paused ones included */
    public List<SyncJob> active()
    {
        return List.copyOf(jobs.values());
    }

    /** 该数据集是否有排队或运行中的同步 / whether the dataset has a queued or running sync */
    public boolean isActive(Long datasetId)
    {
        return activeByDataset.containsKey(datasetId);
    }

    /**
     * 取消没有在运行的同步（CREATED / QUEUE / PAUSED），直接进入 STOPPED 并释放数据集。运行中的返回 false，
     * 由调用方请求执行器停止。
     *
     * Cancel a sync that is not running (queued or paused); false when the executor must stop it.
     */
    public boolean cancelQueued(SyncJob job)
    {
        synchronized (job) {
            RunState state = job.getState();
            if (state != RunState.CREATED && state != RunState.QUEUE && state != RunState.PAUSED) {
                return false;
            }
            if (!transition(job, RunState.STOPPED, state == RunState.PAUSED ? "Cancelled while paused" : "Cancelled before it started")) {
                return false;
            }
        }
        finish(job);
        return true;
    }

    /** 记录最新指标，按时间节流落库；返回本次是否落库 / record metrics; true when they were saved this time */
    public boolean onMetrics(SyncJob job, ExecutorMetrics metrics, boolean force)
    {
        job.setMetrics(metrics);
        if (!force && !job.persistDue(System.currentTimeMillis(), PERSIST_INTERVAL_MS)) {
            return false;
        }
        job.update(history -> applyMetrics(history, metrics));
        return true;
    }

    /**
     * 在执行器上报的指标里登记一个服务端阶段（如刷新表元数据）并立即落库。
     * Record a service-side stage (such as the table metadata refresh) in the job metrics and save it.
     */
    public void stage(SyncJob job, String name, String state, long elapsedMs)
    {
        ExecutorMetrics metrics = job.getMetrics() == null ? new ExecutorMetrics() : job.getMetrics();
        StageMetrics stage = metrics.getStages().stream()
                .filter(item -> name.equals(item.getName()))
                .findFirst()
                .orElseGet(() -> {
                    StageMetrics created = new StageMetrics(name, state, System.currentTimeMillis(), 0);
                    metrics.getStages().add(created);
                    return created;
                });
        stage.setState(state);
        stage.setElapsedMs(elapsedMs);
        onMetrics(job, metrics, true);
    }

    /** 运行中取内存视图，已结束取落库结果 / live view while running, the persisted row afterwards */
    public Optional<SyncMetricsView> view(Long historyId)
    {
        SyncJob job = jobs.get(historyId);
        if (job != null) {
            DatasetHistoryEntity history = job.history();
            synchronized (job) {
                return Optional.of(new SyncMetricsView(historyId, history.getState(), attemptOf(history), true,
                        history.getMessage(), job.getMetrics(), job.getTransitions(), canRetry(history)));
            }
        }
        return historyRepository.findById(historyId)
                .map(history -> new SyncMetricsView(historyId, history.getState(), attemptOf(history), false,
                        history.getMessage(), readMetrics(history.getMetrics()), readTransitions(history.getTransitions()), canRetry(history)));
    }

    boolean transition(SyncJob job, RunState to, String message)
    {
        synchronized (job) {
            RunState from = job.getState();
            if (from == null || !RunStateMachine.canTransition(from, to)) {
                log.warn("Sync history [ {} ] rejected state change {} -> {}", job.getHistoryId(), from, to);
                return false;
            }
            SyncTransition transition = new SyncTransition(from, to, System.currentTimeMillis(), message);
            job.recordTransition(transition);
            job.update(history -> {
                history.setState(to);
                if (message != null) {
                    history.setMessage(message);
                }
                history.setUpdateTime(new Date(transition.getAt()));
                history.setTransitions(writeJson(job.getTransitions()));
            });
            log.info("Sync history [ {} ] {} -> {}", job.getHistoryId(), from, to);
            return true;
        }
    }

    void save(DatasetHistoryEntity history)
    {
        try {
            historyRepository.save(history);
        }
        catch (RuntimeException e) {
            log.warn("Save sync history [ {} ] failed: {}", history.getId(), e.getMessage());
        }
    }

    private void run(SyncJob job)
    {
        if (!transition(job, RunState.RUNNING, null)) {
            // 排队期间已被取消 / cancelled while queued
            return;
        }
        try {
            job.body().accept(job);
        }
        catch (Throwable e) {
            log.warn("Sync history [ {} ] failed", job.getHistoryId(), e);
            transition(job, RunState.FAILURE, e.getMessage());
        }
        finally {
            // 已暂停的同步继续占着数据集，等继续或取消 / a paused sync keeps the dataset until it resumes or is cancelled
            if (job.getState() != RunState.PAUSED) {
                if (!RunStateMachine.isTerminal(job.getState())) {
                    transition(job, RunState.FAILURE, "The sync ended without a final state");
                }
                finish(job);
            }
        }
    }

    private void finish(SyncJob job)
    {
        ExecutorMetrics metrics = job.getMetrics();
        long startedAt = job.getStartedAt();
        job.update(history -> {
            if (metrics != null) {
                applyMetrics(history, metrics);
            }
            // 指标里的耗时不含暂停和排队 / the metrics' elapsed time excludes paused and queued time
            if (metrics != null) {
                history.setElapsed(metrics.getElapsedMs() / 1000);
            }
            else if (startedAt > 0) {
                history.setElapsed((System.currentTimeMillis() - startedAt) / 1000);
            }
        });
        jobs.remove(job.getHistoryId(), job);
        activeByDataset.remove(job.getDatasetId(), job.getHistoryId());
    }

    private void resizePool()
    {
        int size = maxConcurrentSyncs();
        if (size == pool.getMaximumPoolSize()) {
            return;
        }
        if (size > pool.getMaximumPoolSize()) {
            pool.setMaximumPoolSize(size);
            pool.setCorePoolSize(size);
        }
        else {
            pool.setCorePoolSize(size);
            pool.setMaximumPoolSize(size);
        }
        log.info("Dataset sync concurrency set to {}", size);
    }

    int maxConcurrentSyncs()
    {
        try {
            String raw = runtimeConfigureService.getEffective(RuntimeConfigureService.CATEGORY_DATASET, DatasetTargetSchema.NAME, DatasetTargetSchema.fields())
                    .get(MAX_CONCURRENT_SYNCS);
            int value = raw == null ? DEFAULT_MAX_CONCURRENT_SYNCS : Integer.parseInt(raw.trim());
            return value > 0 ? value : DEFAULT_MAX_CONCURRENT_SYNCS;
        }
        catch (RuntimeException e) {
            return DEFAULT_MAX_CONCURRENT_SYNCS;
        }
    }

    private static void applyMetrics(DatasetHistoryEntity history, ExecutorMetrics metrics)
    {
        history.setRowsRead(metrics.getRowsRead());
        history.setRowsWritten(metrics.getRowsWritten());
        history.setRowsSkipped(metrics.getRowsSkipped());
        history.setRowsFailed(metrics.getRowsFailed());
        history.setBytesRead(metrics.getBytesRead());
        history.setBytesWritten(metrics.getBytesWritten());
        history.setProcessedCount(metrics.getRowsWritten());
        if (metrics.getTotalRows() >= 0) {
            history.setTotalCount(metrics.getTotalRows());
        }
        if (metrics.getProgress() >= 0) {
            history.setProgress(BigDecimal.valueOf(metrics.getProgress()).setScale(2, RoundingMode.HALF_UP));
        }
        history.setMetrics(writeJson(metrics));
    }

    private static int attemptOf(DatasetHistoryEntity history)
    {
        return history.getAttempt() == null ? 1 : history.getAttempt();
    }

    private static String writeJson(Object value)
    {
        try {
            return JSON.writeValueAsString(value);
        }
        catch (JsonProcessingException e) {
            log.warn("Serialize sync state failed: {}", e.getMessage());
            return null;
        }
    }

    static ExecutorMetrics readMetrics(String raw)
    {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return JSON.readValue(raw, ExecutorMetrics.class);
        }
        catch (JsonProcessingException e) {
            log.warn("Read sync metrics failed: {}", e.getMessage());
            return null;
        }
    }

    static List<SyncTransition> readTransitions(String raw)
    {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return JSON.readValue(raw, TRANSITIONS);
        }
        catch (JsonProcessingException e) {
            log.warn("Read sync transitions failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
