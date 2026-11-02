package org.devlive.metaforge.server.service;

import org.devlive.metaforge.executor.common.RunState;
import org.devlive.metaforge.executor.configure.ExecutorMetrics;
import org.devlive.metaforge.service.dataset.sync.SyncJob;
import org.devlive.metaforge.service.dataset.sync.SyncJobManager;
import org.devlive.metaforge.service.dataset.sync.SyncMetricsView;
import org.devlive.metaforge.service.dataset.sync.SyncTransition;
import org.devlive.metaforge.service.entity.DatasetHistoryEntity;
import org.devlive.metaforge.service.repository.DatasetHistoryRepository;
import org.devlive.metaforge.service.service.RuntimeConfigureService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SyncJobManagerTest
{
    private final Map<Long, DatasetHistoryEntity> rows = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong();
    private final AtomicInteger saves = new AtomicInteger();
    private SyncJobManager manager;

    @BeforeEach
    public void setUp()
    {
        DatasetHistoryRepository repository = mock(DatasetHistoryRepository.class);
        when(repository.save(any(DatasetHistoryEntity.class))).thenAnswer(invocation -> {
            DatasetHistoryEntity history = invocation.getArgument(0);
            if (history.getId() == null) {
                history.setId(ids.incrementAndGet());
            }
            rows.put(history.getId(), history);
            saves.incrementAndGet();
            return history;
        });
        when(repository.findById(any())).thenAnswer(invocation -> Optional.ofNullable(rows.get((Long) invocation.getArgument(0))));
        RuntimeConfigureService settings = mock(RuntimeConfigureService.class);
        when(settings.getEffective(anyString(), anyString(), anyList()))
                .thenAnswer(invocation -> new java.util.HashMap<>(Map.of(SyncJobManager.MAX_CONCURRENT_SYNCS, "1")));
        manager = new SyncJobManager(repository, settings);
    }

    @AfterEach
    public void tearDown()
    {
        manager.shutdown();
    }

    private DatasetHistoryEntity created(long datasetId)
    {
        DatasetHistoryEntity history = new DatasetHistoryEntity();
        history.setDatasetId(datasetId);
        history.setState(RunState.CREATED);
        history.setAttempt(1);
        return history;
    }

    private Optional<SyncJob> submit(long datasetId, Consumer<SyncJob> body)
    {
        return manager.submit(datasetId, () -> {
            return saveNew(created(datasetId));
        }, body);
    }

    private DatasetHistoryEntity saveNew(DatasetHistoryEntity history)
    {
        history.setId(ids.incrementAndGet());
        rows.put(history.getId(), history);
        return history;
    }

    private static void await(CountDownLatch latch)
    {
        try {
            assertTrue(latch.await(10, TimeUnit.SECONDS), "timed out");
        }
        catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private void awaitState(Long historyId, RunState state)
    {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            if (rows.get(historyId).getState() == state) {
                return;
            }
            Thread.onSpinWait();
        }
        assertEquals(state, rows.get(historyId).getState());
    }

    @Test
    public void aDatasetRunsOneSyncAtATimeAndIsFreedWhenItEnds()
    {
        CountDownLatch release = new CountDownLatch(1);
        SyncJob first = submit(7, job -> {
            await(release);
            job.transition(RunState.SUCCESS, null);
        }).orElseThrow();
        AtomicBoolean factoryCalled = new AtomicBoolean();
        assertTrue(manager.submit(7L, () -> {
            factoryCalled.set(true);
            return saveNew(created(7));
        }, job -> {}).isEmpty());
        assertFalse(factoryCalled.get(), "a rejected submit must not create a history row");
        assertTrue(manager.isActive(7L));

        release.countDown();
        awaitState(first.getHistoryId(), RunState.SUCCESS);
        long deadline = System.currentTimeMillis() + 10_000;
        while (manager.isActive(7L) && System.currentTimeMillis() < deadline) {
            Thread.onSpinWait();
        }
        assertFalse(manager.isActive(7L));
        assertEquals(List.of(RunState.CREATED, RunState.QUEUE, RunState.RUNNING, RunState.SUCCESS),
                manager.view(first.getHistoryId()).orElseThrow().getTransitions().stream().map(SyncTransition::getTo).collect(Collectors.toList()));
    }

    @Test
    public void syncsBeyondTheLimitWaitInTheQueueInOrder()
    {
        CountDownLatch release = new CountDownLatch(1);
        List<Long> started = new java.util.concurrent.CopyOnWriteArrayList<>();
        SyncJob a = submit(1, job -> {
            started.add(1L);
            await(release);
            job.transition(RunState.SUCCESS, null);
        }).orElseThrow();
        awaitState(a.getHistoryId(), RunState.RUNNING);
        SyncJob b = submit(2, job -> {
            started.add(2L);
            job.transition(RunState.SUCCESS, null);
        }).orElseThrow();
        SyncJob c = submit(3, job -> {
            started.add(3L);
            job.transition(RunState.SUCCESS, null);
        }).orElseThrow();
        assertEquals(RunState.QUEUE, b.getState());
        assertEquals(RunState.QUEUE, c.getState());

        release.countDown();
        awaitState(c.getHistoryId(), RunState.SUCCESS);
        assertEquals(List.of(1L, 2L, 3L), started);
    }

    @Test
    public void aQueuedSyncIsCancelledWithoutRunning()
    {
        CountDownLatch release = new CountDownLatch(1);
        SyncJob blocker = submit(1, job -> {
            await(release);
            job.transition(RunState.SUCCESS, null);
        }).orElseThrow();
        awaitState(blocker.getHistoryId(), RunState.RUNNING);
        AtomicBoolean ran = new AtomicBoolean();
        SyncJob queued = submit(2, job -> ran.set(true)).orElseThrow();

        assertTrue(manager.cancelQueued(queued));
        assertEquals(RunState.STOPPED, rows.get(queued.getHistoryId()).getState());
        assertFalse(manager.isActive(2L), "cancelling frees the dataset at once");
        assertFalse(manager.cancelQueued(blocker), "a running sync is not cancelled here");

        release.countDown();
        awaitState(blocker.getHistoryId(), RunState.SUCCESS);
        assertFalse(ran.get());
    }

    @Test
    public void illegalTransitionsAreRejectedAndUnfinishedBodiesFail()
    {
        SyncJob job = submit(5, running -> {
            assertFalse(running.transition(RunState.PAUSED, null), "RUNNING cannot jump to PAUSED");
            assertTrue(running.transition(RunState.STOPPING, null));
            assertFalse(running.transition(RunState.RUNNING, null));
            throw new IllegalStateException("boom");
        }).orElseThrow();
        awaitState(job.getHistoryId(), RunState.FAILURE);
        assertEquals("boom", rows.get(job.getHistoryId()).getMessage());

        SyncJob silent = submit(6, running -> {}).orElseThrow();
        awaitState(silent.getHistoryId(), RunState.FAILURE);
        assertEquals("The sync ended without a final state", rows.get(silent.getHistoryId()).getMessage());
    }

    @Test
    public void metricsAreThrottledAndTheFinalSnapshotIsPersisted()
    {
        CountDownLatch done = new CountDownLatch(1);
        SyncJob job = submit(8, running -> {
            int before = saves.get();
            assertTrue(manager.onMetrics(running, metrics(10, 100), false));
            assertFalse(manager.onMetrics(running, metrics(20, 100), false), "a second report within the interval is not saved");
            assertEquals(before + 1, saves.get());
            assertEquals(20L, manager.view(running.getHistoryId()).orElseThrow().getMetrics().getRowsWritten(), "the live view has the latest report");
            assertTrue(manager.onMetrics(running, metrics(100, 100), true));
            running.transition(RunState.SUCCESS, null);
            done.countDown();
        }).orElseThrow();
        await(done);
        awaitState(job.getHistoryId(), RunState.SUCCESS);
        long deadline = System.currentTimeMillis() + 10_000;
        while (manager.find(job.getHistoryId()).isPresent() && System.currentTimeMillis() < deadline) {
            Thread.onSpinWait();
        }

        DatasetHistoryEntity row = rows.get(job.getHistoryId());
        assertEquals(100L, row.getRowsWritten());
        assertEquals(100L, row.getProcessedCount());
        assertEquals(100L, row.getTotalCount());
        SyncMetricsView view = manager.view(job.getHistoryId()).orElseThrow();
        assertFalse(view.isLive());
        assertEquals(100L, view.getMetrics().getRowsWritten());
        assertEquals(4, view.getTransitions().size());
    }

    private static ExecutorMetrics metrics(long written, long total)
    {
        ExecutorMetrics metrics = new ExecutorMetrics();
        metrics.setRowsRead(written);
        metrics.setRowsWritten(written);
        metrics.setTotalRows(total);
        metrics.setProgress(written * 100.0 / total);
        return metrics;
    }
}
