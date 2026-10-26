package org.devlive.metaforge.server.runner;

import org.devlive.metaforge.executor.common.RunState;
import org.devlive.metaforge.service.entity.DatasetHistoryEntity;
import org.devlive.metaforge.service.initializer.DataSetConfigure;
import org.devlive.metaforge.service.initializer.InstallationState;
import org.devlive.metaforge.service.repository.DatasetHistoryRepository;
import org.devlive.metaforge.service.service.RuntimeConfigureService;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class FirstBootRunnersTest
{
    @Test
    public void testUninstalledInstanceSkipsAllBusinessQueries()
    {
        InstallationState state = mock(InstallationState.class);
        DatasetHistoryRepository history = mock(DatasetHistoryRepository.class);
        RuntimeConfigureService configures = mock(RuntimeConfigureService.class);
        new DataSetConfigure(configures, state).load();
        new DatasetHistoryRecoveryRunner(history, state).run();
        new RuntimeConfigureSeedRunner(null, configures, state).run();
        new SchedulerRunner(null, null, null, state).run();
        ScheduleRunner schedules = new ScheduleRunner(null, null, null, null, null, state);
        try {
            schedules.run();
        }
        finally {
            schedules.shutdown();
        }
        verifyNoInteractions(history, configures);
    }

    @Test
    public void testInstalledInstanceStillRecoversInterruptedHistory()
    {
        InstallationState state = mock(InstallationState.class);
        when(state.isInstalled()).thenReturn(true);
        DatasetHistoryRepository repository = mock(DatasetHistoryRepository.class);
        DatasetHistoryEntity history = new DatasetHistoryEntity();
        history.setState(RunState.RUNNING);
        when(repository.findAllByStateIn(anyList())).thenReturn(Collections.singletonList(history));

        new DatasetHistoryRecoveryRunner(repository, state).run();

        assertEquals(RunState.INTERRUPTED, history.getState());
        verify(repository).saveAll(Collections.singletonList(history));
    }
}
