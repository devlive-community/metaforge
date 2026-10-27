package org.devlive.metaforge.executor.seatunnel;

import org.devlive.metaforge.executor.ExecutorPlugin;
import org.devlive.metaforge.plugin.configure.RuntimeConfigureField;
import org.devlive.metaforge.plugin.configure.RuntimeConfigureFieldType;

import java.util.Arrays;
import java.util.List;

public class SeatunnelExecutor
        extends ExecutorPlugin
{
    @Override
    public List<RuntimeConfigureField> runtimeConfigures()
    {
        return Arrays.asList(
                new RuntimeConfigureField(
                        "home",
                        RuntimeConfigureFieldType.STRING,
                        "/opt/lib/seatunnel",
                        "SeaTunnel installation root (was metaforge.executor.seatunnel.home).",
                        false
                ),
                new RuntimeConfigureField(
                        "startScript",
                        RuntimeConfigureFieldType.STRING,
                        "start-seatunnel-spark-connector-v2.sh",
                        "SeaTunnel launcher script under {home}/bin.",
                        false
                ),
                new RuntimeConfigureField(
                        "engine",
                        RuntimeConfigureFieldType.STRING,
                        "SPARK",
                        "Underlying engine: SPARK / FLINK / SEATUNNEL.",
                        false
                ),
                new RuntimeConfigureField(
                        "way",
                        RuntimeConfigureFieldType.STRING,
                        "LOCAL",
                        "Run way: LOCAL / CLUSTER.",
                        false
                ),
                new RuntimeConfigureField(
                        "mode",
                        RuntimeConfigureFieldType.STRING,
                        "CLIENT",
                        "Run mode: CLIENT / SERVER.",
                        false
                ),
                new RuntimeConfigureField(
                        "data",
                        RuntimeConfigureFieldType.STRING,
                        "",
                        "Executor data buffer path; empty means project root /data.",
                        false
                )
        );
    }
}
