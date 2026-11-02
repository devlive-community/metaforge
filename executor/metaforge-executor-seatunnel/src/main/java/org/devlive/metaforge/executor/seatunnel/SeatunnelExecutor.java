package org.devlive.metaforge.executor.seatunnel;

import org.devlive.metaforge.executor.ExecutorPlugin;
import org.devlive.metaforge.plugin.configure.RuntimeConfigureField;
import org.devlive.metaforge.plugin.configure.RuntimeConfigureFieldType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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

    /** SeaTunnel runs from its installation: the home directory and the launcher under {home}/bin must exist. */
    @Override
    public List<String> testRuntimeConfigures(Map<String, String> values)
    {
        List<String> problems = new ArrayList<>(super.testRuntimeConfigures(values));
        String home = values.getOrDefault("home", "");
        if (home.trim().isEmpty() || !Files.isDirectory(Paths.get(home.trim()))) {
            problems.add("The SeaTunnel home " + home + " is not a directory on the server");
            return problems;
        }
        String script = values.getOrDefault("startScript", "");
        Path launcher = Paths.get(home.trim(), "bin", script.trim());
        if (script.trim().isEmpty() || !Files.isRegularFile(launcher)) {
            problems.add("The launcher " + launcher + " does not exist");
        }
        return problems;
    }
}
