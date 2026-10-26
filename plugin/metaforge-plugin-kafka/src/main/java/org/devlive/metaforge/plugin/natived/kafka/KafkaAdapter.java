package org.devlive.metaforge.plugin.natived.kafka;

import com.google.common.base.Preconditions;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.DescribeConsumerGroupsResult;
import org.devlive.metaforge.spi.adapter.NativeAdapter;
import org.devlive.metaforge.spi.model.Configure;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.Time;
import org.devlive.metaforge.sql.node.element.SelectElement;
import org.devlive.metaforge.sql.statement.SQLStatement;
import org.devlive.metaforge.sql.statement.SelectStatement;
import org.devlive.metaforge.sql.statement.ShowStatement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@SuppressFBWarnings(value = {"RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", "REC_CATCH_EXCEPTION"},
        justification = "I prefer to suppress these FindBugs warnings")
public class KafkaAdapter
        extends NativeAdapter
{
    protected KafkaConnection kafkaConnection;
    private final KafkaParser parser;

    public KafkaAdapter(KafkaConnection kafkaConnection, KafkaParser parser)
    {
        super(kafkaConnection, parser);
        this.kafkaConnection = kafkaConnection;
        this.parser = parser;
    }

    @Override
    public Response handlerExecute(String content)
    {
        Time processorTime = new Time();
        processorTime.setStart(new Date().getTime());
        Response response = this.kafkaConnection.getResponse();
        Configure configure = this.kafkaConnection.getConfigure();
        if (response.getIsConnected()) {
            List<String> headers = new ArrayList<>();
            List<String> types = new ArrayList<>();
            List<Object> columns = new ArrayList<>();
            try {
                SelectStatement statement = (SelectStatement) this.parser.getStatement();
                AdminClient client = this.kafkaConnection.getClient();
                if (!statement.getSelectElements().isEmpty()) {
                    headers.addAll(statement.getSelectElements()
                            .stream()
                            .map(SelectElement::getColumn)
                            .collect(Collectors.toList()));
                }
                else {
                    headers.add("*");
                }
                types.add("String");
                this.adapter(client, parser.getStatement()).forEach(column -> columns.add(Collections.singletonList(column)));
                response.setIsSuccessful(Boolean.TRUE);
            }
            catch (Exception ex) {
                log.error("Execute content failed content {} exception ", content, ex);
                response.setIsSuccessful(Boolean.FALSE);
                response.setMessage(ex.getMessage());
            }
            finally {
                response.setHeaders(headers);
                response.setTypes(types);
                response.setColumns(handlerFormatter(configure.getPluginManager(), configure.getFormat(), headers, columns));
            }
        }
        processorTime.setEnd(new Date().getTime());
        response.setProcessor(processorTime);
        return response;
    }

    private List<String> adapter(AdminClient client, SQLStatement statement)
    {
        List<String> array = new ArrayList<>();
        if (statement instanceof ShowStatement) {
            ShowStatement info = (ShowStatement) statement;

            if (info.getShowType() == ShowStatement.ShowType.DATABASES) {
                this.adapterShowTopics(client, array);
            }
            else if (info.getShowType() == ShowStatement.ShowType.TABLES) {
                this.adapterShowConsumers(client, info, array);
            }
        }
        return array;
    }

    private void adapterShowTopics(AdminClient client, List<String> array)
    {
        try {
            client.listTopics()
                    .listings()
                    .get()
                    .forEach(v -> array.add(v.name()));
        }
        catch (Exception e) {
            Preconditions.checkArgument(false, ExceptionUtils.getMessage(e));
        }
    }

    private void adapterShowConsumers(AdminClient client, ShowStatement info, List<String> array)
    {
        try {
            if (StringUtils.isNotEmpty(info.getTableName())) {
                client.listConsumerGroups()
                        .all()
                        .get()
                        .parallelStream()
                        .forEach(v -> {
                            try {
                                DescribeConsumerGroupsResult describeConsumerGroupsResult = client.describeConsumerGroups(Collections.singleton(v.groupId()));
                                ConsumerGroupDescription consumerGroupDescription = describeConsumerGroupsResult.all().get().get(v.groupId());
                                if (consumerGroupDescription.members().stream().anyMatch(member ->
                                        member.assignment().topicPartitions().stream().anyMatch(tp ->
                                                tp.topic().equals(info.getTableName().replace("`", ""))))) {
                                    array.add(v.groupId());
                                }
                            }
                            catch (Exception e) {
                                Preconditions.checkArgument(false, ExceptionUtils.getMessage(e));
                            }
                        });
            }
            else {
                client.listConsumerGroups()
                        .all()
                        .get()
                        .forEach(v -> array.add(v.groupId()));
            }
        }
        catch (Exception e) {
            Preconditions.checkArgument(false, ExceptionUtils.getMessage(e));
        }
    }
}
