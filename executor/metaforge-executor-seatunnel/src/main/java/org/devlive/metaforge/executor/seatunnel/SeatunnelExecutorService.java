package org.devlive.metaforge.executor.seatunnel;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
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
import org.devlive.metaforge.executor.seatunnel.connector.ConnectorType;
import org.devlive.metaforge.lib.logger.LoggerExecutor;
import org.devlive.metaforge.lib.logger.logback.LogbackExecutor;
import org.devlive.metaforge.lib.shell.ShellCommander;
import org.devlive.metaforge.lib.shell.ShellConfigure;
import org.devlive.metaforge.lib.shell.ShellResponse;
import org.devlive.metaforge.lib.shell.process.ProcessBuilderCommander;
import org.slf4j.Logger;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

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

    /**
     * Writes a child element to the JSON output using the specified type, JSON generator, and executor configure.
     *
     * @param type the type of the child element
     * @param jsonGenerator the JSON generator to write the child element to
     * @param configure the executor configure to use for formatting the child element
     * @throws Exception if there is an error writing the child element
     */
    private void writeChild(String type, JsonGenerator jsonGenerator, ExecutorConfigure configure)
            throws Exception
    {
        jsonGenerator.writeFieldName(type);
        if (ObjectUtils.isNotEmpty(configure)) {
            String protocol = configure.getType();

            if (protocol == null || protocol.trim().isEmpty()) {
                protocol = "Jdbc";
            }
            else {
                try {
                    ConnectorType.valueOf(protocol);
                }
                catch (IllegalArgumentException e) {
                    protocol = configure.getType();
                }
            }

            Connector factory = ConnectorFactory.createFormatter(protocol, configure);
            for (Map.Entry<String, Object> entry : factory.formatToMap().entrySet()) {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeObjectFieldStart(entry.getKey());
                if (entry.getValue() instanceof Properties) {
                    Properties props = (Properties) entry.getValue();
                    for (Map.Entry<Object, Object> property : props.entrySet()) {
                        String key = property.getKey().toString();
                        Object value = property.getValue();

                        if (value == null) {
                            jsonGenerator.writeNullField(key);
                        }
                        else if (value instanceof Boolean) {
                            jsonGenerator.writeBooleanField(key, (Boolean) value);
                        }
                        else if (value instanceof Number) {
                            if (value instanceof Integer) {
                                jsonGenerator.writeNumberField(key, (Integer) value);
                            }
                            else if (value instanceof Long) {
                                jsonGenerator.writeNumberField(key, (Long) value);
                            }
                            else if (value instanceof Double) {
                                jsonGenerator.writeNumberField(key, (Double) value);
                            }
                            else {
                                jsonGenerator.writeStringField(key, value.toString());
                            }
                        }
                        else {
                            String strValue = value.toString();
                            String[] split = strValue.split("\n");
                            if (split.length > 1 && !key.equalsIgnoreCase("sql")) {
                                jsonGenerator.writeArrayFieldStart(key);
                                for (String line : split) {
                                    jsonGenerator.writeString(line);
                                }
                                jsonGenerator.writeEndArray();
                            }
                            else {
                                jsonGenerator.writeStringField(key, strValue);
                            }
                        }
                    }
                }
                jsonGenerator.writeEndObject();
            }
            jsonGenerator.writeEndObject();
        }
        else {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeEndObject();
        }
    }

    /**
     * Generates a configure file in JSON format based on the given ExecutorRequest.
     *
     * @param request the ExecutorRequest object containing the necessary information for generating the configure file
     * @param logger the Logger object for logging any errors that occur during the process
     * @return null if the configure file was generated successfully, or an error message if an exception occurred
     */
    private String before(ExecutorRequest request, Logger logger)
    {
        JsonFactory jsonFactory = new JsonFactory();
        String workFile = String.join(File.separator, request.getWorkHome(), request.getTaskName() + ".configure");
        try (JsonGenerator jsonGenerator = jsonFactory.createGenerator(new File(workFile), JsonEncoding.UTF8)) {
            jsonGenerator.setPrettyPrinter(new DefaultPrettyPrinter());
            jsonGenerator.writeStartObject();
            this.writeChild("env", jsonGenerator, null);
            this.writeChild("source", jsonGenerator, request.getInput());
            if (request.getTransform() != null) {
                this.writeChild("transform", jsonGenerator, request.getTransform());
            }
            this.writeChild("sink", jsonGenerator, request.getOutput());
            jsonGenerator.writeEndObject();
            return null;
        }
        catch (Exception exception) {
            logger.error("Build configure file failed", exception);
            return exception.getMessage();
        }
    }
}
