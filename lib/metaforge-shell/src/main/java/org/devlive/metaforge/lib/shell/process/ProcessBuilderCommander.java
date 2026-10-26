package org.devlive.metaforge.lib.shell.process;

import ch.qos.logback.classic.Logger;
import com.google.common.base.Preconditions;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.lib.logger.LoggerExecutor;
import org.devlive.metaforge.lib.shell.ShellCommander;
import org.devlive.metaforge.lib.shell.ShellConfigure;
import org.devlive.metaforge.lib.shell.ShellResponse;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@SuppressFBWarnings(value = {"REC_CATCH_EXCEPTION"},
        justification = "I prefer to suppress these FindBugs warnings")
@Slf4j
public class ProcessBuilderCommander
        extends ShellCommander
{
    public ProcessBuilderCommander(ShellConfigure configure)
    {
        super(configure);
    }

    @Override
    public ShellResponse execute()
    {
        LoggerExecutor loggerExecutor = this.configure.getLoggerExecutor();
        Logger logger = (Logger) loggerExecutor.getLogger();
        logger.info("Execute task on username [ {} ]", configure.getUsername());
        ShellResponse shellResponse = new ShellResponse();
        shellResponse.setSuccessful(Boolean.TRUE);
        List<String> command = new ArrayList<>();
        if (StringUtils.isNotEmpty(getShellType())) {
            command.add(getShellType());
            command.add("-c");
        }
        command.addAll(configure.getCommand());
        ProcessBuilder builder = new ProcessBuilder();
        builder.command(command);
        logger.info("Execute command {}", String.join(" ", command));
        builder.directory(new File(configure.getDirectory()));
        logger.info("Work directory {}", configure.getDirectory());
        Map<String, String> environment = this.getEnvironment();
        builder.environment().putAll(environment);
        logger.info("========== container environment start ==========");
        environment.keySet().forEach(key -> logger.info("Container environment {}={}", key, environment.get(key)));
        logger.info("========== container environment end ==========");

        Process process = null;
        try {
            process = builder.start();
            boolean exitCode = process.waitFor(configure.getTimeout(), TimeUnit.SECONDS);
            if (!exitCode) {
                logger.info(String.format("Timeout [ %s ] seconds killed", configure.getTimeout()));
                process.destroy();
                shellResponse.setSuccessful(Boolean.FALSE);
                shellResponse.setTimeout(true);
                Preconditions.checkArgument(false, String.format("Timeout [ %s ] seconds", configure.getTimeout()));
            }

            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while (process.isAlive()) {
                    while (bufferedReader.ready()) {
                        while ((line = bufferedReader.readLine()) != null) {
                            if (line.toUpperCase().contains("ERROR")) {
                                logger.error(line);
                            }
                            logger.info(line);
                        }
                    }
                }
            }

            shellResponse.setCode(process.exitValue());
            if (process.exitValue() > 0) {
                List<String> errors = new ArrayList<>();
                try (InputStreamReader streamReader = new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8);
                        BufferedReader reader = new BufferedReader(streamReader)) {
                    reader.lines()
                            .forEach(line -> {
                                logger.error(line);
                                errors.add(line);
                            });
                    shellResponse.setErrors(errors);
                    shellResponse.setSuccessful(Boolean.FALSE);
                }
            }

            logger.info("Execute task exit code [ {} ]", process.exitValue());
            if (process.exitValue() == 0) {
                shellResponse.setErrors(null);
                shellResponse.setSuccessful(Boolean.TRUE);
            }
        }
        catch (Exception ex) {
            logger.error("Execute failed ", ex);
            shellResponse.setSuccessful(Boolean.FALSE);
        }
        finally {
            if (ObjectUtils.isNotEmpty(process)) {
                process.destroy();
            }
            logger.info("Destroy logger executor");
            loggerExecutor.destroy();
        }
        log.info("Execute task on username [ {} ] finished", configure.getUsername());
        return shellResponse;
    }
}
