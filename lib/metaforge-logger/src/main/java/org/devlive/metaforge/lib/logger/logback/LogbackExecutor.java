package org.devlive.metaforge.lib.logger.logback;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.util.OptionHelper;
import org.apache.commons.lang3.ObjectUtils;
import org.devlive.metaforge.lib.logger.LoggerExecutor;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LogbackExecutor
        extends LoggerExecutor<Logger>
{
    private static final Map<String, FileAppender> APPENDER_CONTAINER = new HashMap<>();
    private static final Map<String, PatternLayoutEncoder> ENCODER_CONTAINER = new HashMap<>();

    public LogbackExecutor(String directory, String name)
    {
        super(directory, name);
    }

    @Override
    public Logger getLogger()
    {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        FileAppender fileAppender = createFileAppender(loggerContext);
        Logger logger = (Logger) LoggerFactory.getLogger(this.name);
        logger.addAppender(fileAppender);
        logger.setAdditive(false);
        return logger;
    }

    @Override
    public void destroy()
    {
        FileAppender appender = APPENDER_CONTAINER.get(this.name);
        if (ObjectUtils.isNotEmpty(appender)) {
            if (appender.isStarted()) {
                appender.stop();
            }
            APPENDER_CONTAINER.remove(this.name);
        }

        PatternLayoutEncoder encoder = ENCODER_CONTAINER.get(this.name);
        if (ObjectUtils.isNotEmpty(encoder)) {
            if (encoder.isStarted()) {
                encoder.stop();
            }
            ENCODER_CONTAINER.remove(this.name);
        }
    }

    private FileAppender createFileAppender(LoggerContext loggerContext)
    {
        FileAppender<ILoggingEvent> appender = new FileAppender<>();
        APPENDER_CONTAINER.put(this.name, appender);
        appender.setContext(loggerContext);
        appender.setName(this.name);
        String logPath = OptionHelper.substVars(String.join("/", this.directory, this.name), loggerContext);
        appender.setFile(logPath);
        appender.setAppend(true);
        appender.setPrudent(false);
        appender.setEncoder(createEncoder(loggerContext));
        appender.start();
        return appender;
    }

    private PatternLayoutEncoder createEncoder(LoggerContext context)
    {
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        ENCODER_CONTAINER.put(this.name, encoder);
        encoder.setContext(context);
        String pattern = "%date %level [%thread] [%file:%line] %msg%n";
        encoder.setPattern(pattern);
        encoder.setCharset(StandardCharsets.UTF_8);
        encoder.start();
        return encoder;
    }
}
