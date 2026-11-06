package org.devlive.metaforge.executor.seatunnel;

import org.devlive.metaforge.executor.configure.ExecutorConfigure;
import org.devlive.metaforge.executor.configure.ExecutorRequest;
import org.devlive.metaforge.executor.workflow.WorkflowNode;
import org.devlive.metaforge.executor.workflow.WorkflowPlan;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

/**
 * Compiles a workflow pipeline into a SeaTunnel job: every node becomes a connector or transform
 * block, and consecutive blocks are joined through result table names.
 *
 * <p>The table between two components is named by the consumer's {@code source_table_name}, else
 * the producer's {@code result_table_name}, else a generated name. {@code ${input}} in a transform
 * query stands for its incoming table.</p>
 */
final class SeatunnelWorkflowCompiler
{
    static final String SOURCE_TABLE = "source_table_name";
    static final String RESULT_TABLE = "result_table_name";
    static final String INPUT_PLACEHOLDER = "${input}";
    /** Options prefixed with this are written to the SeaTunnel env block. */
    static final String ENV_PREFIX = "env.";

    private SeatunnelWorkflowCompiler()
    {}

    static List<String> validate(WorkflowPlan plan)
    {
        List<String> errors = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        for (WorkflowNode transform : plan.getTransforms()) {
            // A job is one object keyed by plugin name, so the same transform cannot appear twice.
            if (!keys.add(transform.getKey())) {
                errors.add(transform.getLabel() + ": SeaTunnel cannot run the same transform type twice in one pipeline");
            }
        }
        List<WorkflowNode> chain = plan.getNodes();
        for (int index = 0; index + 1 < chain.size(); index++) {
            String produced = chain.get(index).string(RESULT_TABLE);
            String consumed = chain.get(index + 1).string(SOURCE_TABLE);
            if (produced != null && consumed != null && !produced.equals(consumed)) {
                errors.add(chain.get(index + 1).getLabel() + ": the source table name must match the result table name of "
                        + chain.get(index).getLabel());
            }
        }
        return errors;
    }

    static ExecutorRequest compile(WorkflowPlan plan)
    {
        List<String> errors = validate(plan);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
        }
        WorkflowNode sourceNode = plan.source();
        WorkflowNode sinkNode = plan.sink();
        List<WorkflowNode> chain = new ArrayList<>();
        chain.add(sourceNode);
        chain.addAll(plan.getTransforms());
        chain.add(sinkNode);

        List<Properties> blocks = new ArrayList<>();
        chain.forEach(node -> blocks.add(properties(node)));
        int last = chain.size() - 1;
        if (last > 1) {
            // Only wire table names when transforms sit in between, as SeaTunnel does by default.
            for (int edge = 0; edge < last; edge++) {
                String table = tableName(chain.get(edge), chain.get(edge + 1), edge, last);
                blocks.get(edge).put(RESULT_TABLE, table);
                blocks.get(edge + 1).put(SOURCE_TABLE, table);
                Object query = blocks.get(edge + 1).get("query");
                if (edge + 1 < last && query != null) {
                    blocks.get(edge + 1).put("query", query.toString().replace(INPUT_PLACEHOLDER, table));
                }
            }
        }

        List<ExecutorConfigure> transforms = new ArrayList<>();
        for (int index = 1; index < last; index++) {
            transforms.add(new ExecutorConfigure(chain.get(index).getKey(), blocks.get(index), Set.of()));
        }
        ExecutorRequest request = new ExecutorRequest(plan.getTaskName(), plan.getUserName(),
                new ExecutorConfigure(sourceNode.getKey(), blocks.get(0), Set.of()),
                new ExecutorConfigure(sinkNode.getKey(), blocks.get(last), Set.of()));
        request.setWorkHome(plan.getWorkHome());
        request.setTimeout(plan.getTimeout());
        request.setTransforms(transforms);
        request.setTransform(transforms.isEmpty() ? null : transforms.get(0));
        Map<String, String> options = new LinkedHashMap<>(plan.getOptions());
        Object parallelism = plan.getSettings().get("parallelism");
        if (parallelism != null && !parallelism.toString().isBlank()) {
            options.put(ENV_PREFIX + "parallelism", number(parallelism));
        }
        request.setOptions(options);
        return request;
    }

    private static String tableName(WorkflowNode producer, WorkflowNode consumer, int edge, int last)
    {
        String consumed = consumer.string(SOURCE_TABLE);
        if (consumed != null) {
            return consumed;
        }
        String produced = producer.string(RESULT_TABLE);
        if (produced != null) {
            return produced;
        }
        return edge == 0 ? "workflow_input" : edge == last - 1 ? "workflow_output" : "workflow_" + edge;
    }

    /** Typed values are kept: numbers, booleans, lists and maps must not be quoted in the job. */
    private static Properties properties(WorkflowNode node)
    {
        Properties properties = new Properties();
        node.getValues().forEach((key, value) -> {
            if (value != null && (!(value instanceof String) || !((String) value).isBlank())) {
                properties.put(key, value);
            }
        });
        return properties;
    }

    private static String number(Object value)
    {
        if (value instanceof Number && ((Number) value).doubleValue() == Math.rint(((Number) value).doubleValue())) {
            return String.valueOf(((Number) value).longValue());
        }
        return Objects.toString(value);
    }
}
