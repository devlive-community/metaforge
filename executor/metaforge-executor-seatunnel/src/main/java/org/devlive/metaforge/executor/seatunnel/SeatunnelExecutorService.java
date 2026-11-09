package org.devlive.metaforge.executor.seatunnel;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.executor.ExecutorService;
import org.devlive.metaforge.executor.common.RunEngine;
import org.devlive.metaforge.executor.common.RunMode;
import org.devlive.metaforge.executor.common.RunState;
import org.devlive.metaforge.executor.common.RunWay;
import org.devlive.metaforge.executor.configure.ExecutorConfigure;
import org.devlive.metaforge.executor.configure.ExecutorRequest;
import org.devlive.metaforge.executor.configure.ExecutorResponse;
import org.devlive.metaforge.executor.seatunnel.connector.Connector;
import org.devlive.metaforge.executor.seatunnel.connector.ConnectorFactory;
import org.devlive.metaforge.executor.workflow.WorkflowPlan;
import org.devlive.metaforge.lib.logger.LoggerExecutor;
import org.devlive.metaforge.lib.logger.logback.LogbackExecutor;
import org.devlive.metaforge.lib.shell.ShellCommander;
import org.devlive.metaforge.lib.shell.ShellConfigure;
import org.devlive.metaforge.lib.shell.ShellResponse;
import org.devlive.metaforge.lib.shell.process.ProcessBuilderCommander;
import org.slf4j.Logger;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

@SuppressFBWarnings(value = {"RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE"})
@Slf4j
public class SeatunnelExecutorService
        implements ExecutorService
{
    @Override
    public ExecutorResponse start(ExecutorRequest request)
    {
        try {
            SeaTunnelCommander commander = buildCommander(request);

            LoggerExecutor loggerExecutor = new LogbackExecutor(request.getWorkHome(), request.getTaskName() + ".log");
            String result = before(request, loggerExecutor.getLogger());
            if (StringUtils.isNotEmpty(result)) {
                return new ExecutorResponse(false, false, RunState.FAILURE, result);
            }

            ShellConfigure shellConfigure = ShellConfigure.builder()
                    .directory(request.getWorkHome())
                    .loggerExecutor(loggerExecutor)
                    .command(Collections.singletonList(commander.toCommand()))
                    .timeout(request.getTimeout())
                    .username(request.getUserName())
                    .build();
            ShellCommander shellExecutor = new ProcessBuilderCommander(shellConfigure);
            ShellResponse response = shellExecutor.execute();
            log.info("Task [ {} ] executed code [ {} ]", request.getTaskName(), response.getCode());
            RunState state = response.getSuccessful() ? RunState.SUCCESS : RunState.FAILURE;
            log.info("Task [ {} ] executed state [ {} ]", request.getTaskName(), state);

            String message = null;
            if (response.getErrors() != null && !response.getSuccessful()) {
                message = String.join("\n", response.getErrors());
            }
            return new ExecutorResponse(response.isTimeout(), response.getSuccessful(), state, message);
        }
        catch (Exception exception) {
            return new ExecutorResponse(false, false, RunState.FAILURE, exception.getMessage());
        }
    }

    @Override
    public ExecutorResponse stop(ExecutorRequest request)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * 从请求构造 SeaTunnel 命令。单独抽出以便测试“请求字段 -> 命令行”的映射，
     * 这是 ExecutorRequest 结构调整时最容易出错、也最需要回归保护的一段。
     */
    SeaTunnelCommander buildCommander(ExecutorRequest request)
    {
        // Seatunnel 专属项来自 request.options：home / startScript / way / mode / engine
        Map<String, String> options = request.getOptions();
        RunWay runWay = RunWay.valueOf(options.getOrDefault("way", RunWay.LOCAL.name()));
        RunMode runMode = RunMode.valueOf(options.getOrDefault("mode", RunMode.CLIENT.name()));
        RunEngine runEngine = RunEngine.valueOf(options.getOrDefault("engine", RunEngine.SPARK.name()));
        return new SeaTunnelCommander(
                options.get("home") + "/bin",
                options.get("startScript"),
                runWay.name().toLowerCase(),
                runMode.name().toLowerCase(),
                String.join(File.separator, request.getWorkHome(), request.getTaskName() + ".configure"),
                request.getTaskName(),
                runEngine);
    }

    @Override
    public List<String> validate(WorkflowPlan plan)
    {
        return SeatunnelWorkflowCompiler.validate(plan);
    }

    @Override
    public ExecutorRequest compile(WorkflowPlan plan)
    {
        return SeatunnelWorkflowCompiler.compile(plan);
    }

    /**
     * Writes one job block ({@code source}, {@code transform} or {@code sink}) holding one entry per
     * configured plugin, keyed by the SeaTunnel plugin name.
     */
    private void writeBlock(String type, JsonGenerator jsonGenerator, List<ExecutorConfigure> configures)
            throws Exception
    {
        jsonGenerator.writeFieldName(type);
        jsonGenerator.writeStartObject();
        for (ExecutorConfigure configure : configures) {
            String protocol = configure.getType();
            if (protocol == null || protocol.trim().isEmpty()) {
                protocol = "Jdbc";
            }
            Connector factory = ConnectorFactory.createFormatter(protocol, configure);
            for (Map.Entry<String, Object> entry : factory.formatToMap().entrySet()) {
                jsonGenerator.writeObjectFieldStart(entry.getKey());
                if (entry.getValue() instanceof Properties) {
                    Properties props = (Properties) entry.getValue();
                    for (Map.Entry<Object, Object> property : props.entrySet()) {
                        // Preserve arrays, nested connection properties, booleans and numeric values.
                        // In particular, multiline SQL is a string rather than an array of lines.
                        jsonGenerator.writeObjectField(property.getKey().toString(), property.getValue());
                    }
                }
                jsonGenerator.writeEndObject();
            }
        }
        jsonGenerator.writeEndObject();
    }

    /** Writes the env block from options prefixed with {@code env.}; numeric and boolean values stay typed. */
    private void writeEnv(JsonGenerator jsonGenerator, Map<String, String> options)
            throws Exception
    {
        jsonGenerator.writeFieldName("env");
        jsonGenerator.writeStartObject();
        for (Map.Entry<String, String> option : new TreeMap<>(options).entrySet()) {
            if (!option.getKey().startsWith(SeatunnelWorkflowCompiler.ENV_PREFIX) || option.getValue() == null) {
                continue;
            }
            String key = option.getKey().substring(SeatunnelWorkflowCompiler.ENV_PREFIX.length());
            String value = option.getValue().trim();
            if (value.matches("-?\\d+")) {
                jsonGenerator.writeNumberField(key, Long.parseLong(value));
            }
            else if (value.equals("true") || value.equals("false")) {
                jsonGenerator.writeBooleanField(key, Boolean.parseBoolean(value));
            }
            else {
                jsonGenerator.writeStringField(key, value);
            }
        }
        jsonGenerator.writeEndObject();
    }

    /**
     * Generates a configure file in JSON format based on the given ExecutorRequest.
     *
     * @param request the ExecutorRequest object containing the necessary information for generating the configure file
     * @param logger the Logger object for logging any errors that occur during the process
     * @return null if the configure file was generated successfully, or an error message if an exception occurred
     */
    String before(ExecutorRequest request, Logger logger)
    {
        JsonFactory jsonFactory = new ObjectMapper().getFactory();
        String workFile = String.join(File.separator, request.getWorkHome(), request.getTaskName() + ".configure");
        try (JsonGenerator jsonGenerator = jsonFactory.createGenerator(new File(workFile), JsonEncoding.UTF8)) {
            jsonGenerator.setPrettyPrinter(new DefaultPrettyPrinter());
            jsonGenerator.writeStartObject();
            this.writeEnv(jsonGenerator, request.getOptions());
            this.writeBlock("source", jsonGenerator, List.of(request.getInput()));
            List<ExecutorConfigure> transforms = request.transformChain();
            if (!transforms.isEmpty()) {
                this.writeBlock("transform", jsonGenerator, transforms);
            }
            this.writeBlock("sink", jsonGenerator, List.of(request.getOutput()));
            jsonGenerator.writeEndObject();
            return null;
        }
        catch (Exception exception) {
            logger.error("Build configure file failed", exception);
            return exception.getMessage();
        }
    }
}
