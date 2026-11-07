package org.devlive.metaforge.test.spi

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.executor.common.RunState
import org.devlive.metaforge.executor.common.RunStateMachine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@SuppressFBWarnings(value = ["BC_BAD_CAST_TO_ABSTRACT_COLLECTION"])
class RunStateMachineTest
{
    private fun allowed(from: RunState, vararg to: RunState)
    {
        to.forEach { assertTrue("$from -> $it should be allowed", RunStateMachine.canTransition(from, it)) }
    }

    private fun rejected(from: RunState, vararg to: RunState)
    {
        to.forEach { assertFalse("$from -> $it should be rejected", RunStateMachine.canTransition(from, it)) }
    }

    @Test
    fun queuedTasksStartOrAreCancelled()
    {
        allowed(RunState.CREATED, RunState.QUEUE, RunState.STOPPED, RunState.FAILURE)
        allowed(RunState.QUEUE, RunState.RUNNING, RunState.STOPPED)
        rejected(RunState.QUEUE, RunState.SUCCESS, RunState.PAUSED, RunState.STOPPING)
        rejected(RunState.CREATED, RunState.RUNNING, RunState.SUCCESS)
    }

    @Test
    fun runningTasksPauseStopRetryOrFinish()
    {
        allowed(RunState.RUNNING, RunState.PAUSING, RunState.STOPPING, RunState.RETRYING,
            RunState.SUCCESS, RunState.FAILURE, RunState.TIMEOUT, RunState.STOPPED)
        allowed(RunState.PAUSING, RunState.PAUSED, RunState.STOPPING, RunState.SUCCESS)
        allowed(RunState.RUNNING, RunState.PAUSED)
        allowed(RunState.PAUSED, RunState.QUEUE, RunState.STOPPED)
        rejected(RunState.PAUSED, RunState.RUNNING, RunState.SUCCESS)
        allowed(RunState.STOPPING, RunState.STOPPED, RunState.SUCCESS, RunState.FAILURE)
        rejected(RunState.STOPPING, RunState.RUNNING, RunState.PAUSED)
        allowed(RunState.RETRYING, RunState.RUNNING, RunState.STOPPED)
    }

    @Test
    fun finishedTasksOnlyRequeueAndSuccessIsFinal()
    {
        listOf(RunState.FAILURE, RunState.TIMEOUT, RunState.STOPPED, RunState.INTERRUPTED).forEach {
            allowed(it, RunState.QUEUE)
            rejected(it, RunState.RUNNING, RunState.SUCCESS)
        }
        RunState.values().forEach { rejected(RunState.SUCCESS, it) }
    }

    @Test
    fun everyActiveStateCanBeInterruptedAndTheTerminalSetIsExact()
    {
        val active = RunStateMachine.activeStates()
        assertEquals(
            setOf(RunState.CREATED, RunState.QUEUE, RunState.RUNNING, RunState.STOPPING,
                RunState.PAUSING, RunState.PAUSED, RunState.RETRYING),
            active.toSet()
        )
        active.forEach { allowed(it, RunState.INTERRUPTED) }
        RunState.values().filterNot { it in active }.forEach { assertTrue(RunStateMachine.isTerminal(it)) }
    }
}
