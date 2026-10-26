package org.devlive.metaforge.executor.seatunnel;

import org.devlive.metaforge.executor.ExecutorPlugin;
import org.devlive.metaforge.plugin.configure.PluginConfigureField;
import org.devlive.metaforge.plugin.configure.PluginFieldType;

import java.util.Arrays;
import java.util.List;

public class SeatunnelExecutor
        extends ExecutorPlugin
{
    @Override
    public List<PluginConfigureField> configures()
    {
        return Arrays.asList(
                new PluginConfigureField(
                        "home",
                        PluginFieldType.STRING,
                        "/opt/lib/seatunnel",
                        "SeaTunnel installation root (was metaforge.executor.seatunnel.home).",
                        false
                ),
                new PluginConfigureField(
                        "startScript",
                        PluginFieldType.STRING,
                        "start-seatunnel-spark-connector-v2.sh",
                        "SeaTunnel launcher script under {home}/bin.",
                        false
                ),
                new PluginConfigureField(
                        "engine",
                        PluginFieldType.STRING,
                        "SPARK",
                        "Underlying engine: SPARK / FLINK / SEATUNNEL.",
                        false
                ),
                new PluginConfigureField(
                        "way",
                        PluginFieldType.STRING,
                        "LOCAL",
                        "Run way: LOCAL / CLUSTER.",
                        false
                ),
                new PluginConfigureField(
                        "mode",
                        PluginFieldType.STRING,
                        "CLIENT",
                        "Run mode: CLIENT / SERVER.",
                        false
                ),
                new PluginConfigureField(
                        "data",
                        PluginFieldType.STRING,
                        "",
                        "Executor data buffer path; empty means project root /data.",
                        false
                )
        );
    }
}
