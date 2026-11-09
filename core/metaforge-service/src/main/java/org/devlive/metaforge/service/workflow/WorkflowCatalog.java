package org.devlive.metaforge.service.workflow;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.executor.ExecutorService;
import org.devlive.metaforge.executor.configure.ExecutorConfigure;
import org.devlive.metaforge.executor.workflow.WorkflowCategory;
import org.devlive.metaforge.executor.workflow.WorkflowNode;
import org.devlive.metaforge.executor.workflow.WorkflowPlan;
import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.plugin.PluginMetadata;
import org.devlive.metaforge.plugin.PluginType;
import org.devlive.metaforge.service.entity.WorkflowEntity;
import org.devlive.metaforge.service.itransient.configuration.Configuration;
import org.devlive.metaforge.service.itransient.configuration.ConnectionConfiguration;
import org.devlive.metaforge.service.itransient.configuration.NodeConfiguration;
import org.devlive.metaforge.service.itransient.configuration.NodeItemConfiguration;
import org.devlive.metaforge.service.itransient.configuration.PortConfiguration;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

/**
 * The server is the authority for the node palette and executable graph shapes. Executors are
 * discovered from the installed executor plugins; each one describes its capabilities, limits, job
 * settings and node plugins in its own declaration, and compiles validated graphs itself. Nothing
 * here depends on a particular executor.
 */
@Slf4j
@Component
public class WorkflowCatalog
{
    public static final int MAX_NODES = 100;
    public static final int MAX_CONNECTIONS = 200;
    public static final long MAX_TIMEOUT = 86400;

    private final PluginManager plugins;
    // Declarations ship inside the plugin; a reloaded plugin brings a new service instance and is read again.
    private final Map<ExecutorService, Optional<WorkflowExecutorDefinition>> declarations = Collections.synchronizedMap(new WeakHashMap<>());

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The plugin manager is an intentionally shared Spring dependency")
    public WorkflowCatalog(PluginManager plugins)
    {
        this.plugins = plugins;
    }

    /** Resolves a saved data source code for a node field of type {@code source}. */
    @FunctionalInterface
    public interface SourceResolver
    {
        ExecutorConfigure resolve(String code);
    }

    /** An available executor: its plugin, service and declaration. */
    public static final class WorkflowExecutor
    {
        private final Plugin plugin;
        private final ExecutorService service;
        private final WorkflowExecutorDefinition definition;

        private WorkflowExecutor(Plugin plugin, ExecutorService service, WorkflowExecutorDefinition definition)
        {
            this.plugin = plugin;
            this.service = service;
            this.definition = definition;
        }

        @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Plugins are shared runtime objects")
        public Plugin plugin()
        {
            return plugin;
        }

        public ExecutorService service()
        {
            return service;
        }

        @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Declarations are read-only catalog data")
        public WorkflowExecutorDefinition definition()
        {
            return definition;
        }
    }

    public Map<String, Object> catalog()
    {
        return Map.of("executors", executors().stream().map(WorkflowExecutor::definition).collect(Collectors.toList()));
    }

    /** Executors whose plugin is running and ships a workflow declaration, ordered by name. */
    public List<WorkflowExecutor> executors()
    {
        List<WorkflowExecutor> result = new ArrayList<>();
        for (PluginMetadata metadata : plugins.getPluginInfos()) {
            if (metadata.getType() == PluginType.EXECUTOR && metadata.getName() != null) {
                find(metadata.getName()).ifPresent(result::add);
            }
        }
        result.sort(Comparator.comparing(executor -> executor.definition().getName()));
        return result;
    }

    public WorkflowExecutor executor(String name)
    {
        return find(name).orElseThrow(() -> new IllegalArgumentException("Executor is unavailable: " + name));
    }

    public List<NodeConfiguration> definitions(String executor)
    {
        return executor(executor).definition().getNodes();
    }

    private Optional<WorkflowExecutor> find(String name)
    {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        Optional<Plugin> plugin = plugins.getPlugin(name);
        if (plugin.isEmpty() || plugin.get().getType() != PluginType.EXECUTOR) {
            return Optional.empty();
        }
        ExecutorService service;
        try {
            service = plugin.get().getService(ExecutorService.class);
        }
        catch (RuntimeException e) {
            log.warn("Executor plugin {} has no executor service", name, e);
            return Optional.empty();
        }
        if (service == null) {
            return Optional.empty();
        }
        String executorName = plugin.get().getName();
        return declaration(executorName, service).map(definition -> new WorkflowExecutor(plugin.get(), service, definition));
    }

    private Optional<WorkflowExecutorDefinition> declaration(String name, ExecutorService service)
    {
        return declarations.computeIfAbsent(service, ignored -> {
            URL location = service.workflowDeclaration();
            if (location == null) {
                return Optional.empty();
            }
            try {
                return Optional.of(WorkflowDeclarationReader.read(name, location));
            }
            catch (IOException | RuntimeException e) {
                log.warn("Cannot read the workflow declaration of executor {} at {}", name, location, e);
                return Optional.empty();
            }
        });
    }

    public List<String> validate(WorkflowEntity workflow)
    {
        List<String> errors = new ArrayList<>();
        try {
            WorkflowExecutor executor = executor(workflow.getExecutor());
            WorkflowExecutorDefinition definition = executor.definition();
            Configuration graph = workflow.getConfigure();
            if (graph == null || graph.getNodes() == null || graph.getNodes().isEmpty()) {
                return List.of("Add an input and an output node");
            }
            if (graph.getNodes().size() > MAX_NODES || graph.getConnections() != null && graph.getConnections().size() > MAX_CONNECTIONS) {
                return List.of("Workflow is too large");
            }
            if (graph.getTimeout() != null && (graph.getTimeout() < 1 || graph.getTimeout() > MAX_TIMEOUT)) {
                errors.add("Timeout must be between 1 and " + MAX_TIMEOUT + " seconds");
            }
            errors.addAll(fieldErrors("Settings", definition.getSettings(), graph.getSettings()));
            Map<String, NodeConfiguration> allowed = definition.getNodes().stream()
                    .collect(Collectors.toMap(NodeConfiguration::getId, node -> node, (left, right) -> left));
            Map<String, NodeConfiguration> nodes = new LinkedHashMap<>();
            for (NodeConfiguration node : graph.getNodes()) {
                if (node.getId() == null || nodes.putIfAbsent(node.getId(), node) != null) {
                    errors.add("Node identifiers must be present and unique");
                }
                NodeConfiguration type = allowed.get(node.getTid());
                if (type == null || !Objects.equals(type.getCategory(), node.getCategory()) || !Objects.equals(type.getKey(), node.getKey())) {
                    errors.add("Unsupported node for " + definition.getName() + ": " + node.getTid());
                    continue;
                }
                errors.addAll(fieldErrors(label(node), type.getConfigure(), node.getData()));
            }
            for (String category : WorkflowExecutorDefinition.CATEGORIES) {
                long count = graph.getNodes().stream().filter(node -> category.equals(node.getCategory())).count();
                int minimum = "transform".equals(category) ? 0 : 1;
                if (count < minimum || count > definition.limit(category)) {
                    errors.add("source".equals(category) || "sink".equals(category)
                            ? "This executor requires " + (definition.limit(category) == 1 ? "exactly one " : "at least one ") + ("source".equals(category) ? "input" : "output")
                            : "This executor supports at most " + definition.limit(category) + " transforms");
                }
            }
            if (!errors.isEmpty()) {
                return errors;
            }
            List<NodeConfiguration> chain = pipeline(graph, allowed, errors);
            if (errors.isEmpty() && chain != null) {
                errors.addAll(executor.service().validate(plan(workflow, chain, definition, null, "", "", Map.of())));
            }
        }
        catch (IllegalArgumentException e) {
            errors.add(e.getMessage());
        }
        return errors;
    }

    /**
     * Builds the plan an executor compiles. The workflow must have passed {@link #validate}; saved
     * data sources referenced by {@code source} fields are resolved through {@code resolver}.
     */
    public WorkflowPlan plan(WorkflowEntity workflow, String taskName, String userName, Map<String, String> options, SourceResolver resolver)
    {
        WorkflowExecutorDefinition definition = executor(workflow.getExecutor()).definition();
        Map<String, NodeConfiguration> allowed = definition.getNodes().stream()
                .collect(Collectors.toMap(NodeConfiguration::getId, node -> node, (left, right) -> left));
        List<String> errors = new ArrayList<>();
        List<NodeConfiguration> chain = pipeline(workflow.getConfigure(), allowed, errors);
        if (chain == null || !errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
        }
        return plan(workflow, chain, definition, resolver, taskName, userName, options);
    }

    private WorkflowPlan plan(WorkflowEntity workflow, List<NodeConfiguration> chain, WorkflowExecutorDefinition definition,
            SourceResolver resolver, String taskName, String userName, Map<String, String> options)
    {
        Map<String, NodeConfiguration> types = definition.getNodes().stream()
                .collect(Collectors.toMap(NodeConfiguration::getId, node -> node, (left, right) -> left));
        List<WorkflowNode> nodes = new ArrayList<>();
        for (NodeConfiguration node : chain) {
            Map<String, Object> values = node.getData() == null ? Map.of() : new LinkedHashMap<>(node.getData());
            Map<String, ExecutorConfigure> sources = new LinkedHashMap<>();
            NodeConfiguration type = types.get(node.getTid());
            if (resolver != null && type != null) {
                for (NodeItemConfiguration field : type.getConfigure()) {
                    Object code = values.get(field.getField());
                    if ("source".equals(field.getType()) && code != null && !code.toString().isBlank()) {
                        sources.put(field.getField(), resolver.resolve(code.toString()));
                    }
                }
            }
            WorkflowCategory category = WorkflowCategory.of(node.getCategory());
            if (category == null) {
                throw new IllegalArgumentException("Unsupported node category: " + node.getCategory());
            }
            nodes.add(new WorkflowNode(node.getId(), node.getTid(), node.getKey(), category, label(node), values, sources));
        }
        Configuration graph = workflow.getConfigure();
        Map<String, Object> settings = graph.getSettings() == null ? Map.of() : new LinkedHashMap<>(graph.getSettings());
        long timeout = Optional.ofNullable(graph.getTimeout()).orElse(600L);
        return new WorkflowPlan(taskName, userName, workflow.getWork(), timeout, nodes, settings, options);
    }

    /** Orders a single input-to-output chain, reporting connection and topology errors. */
    private List<NodeConfiguration> pipeline(Configuration graph, Map<String, NodeConfiguration> types, List<String> errors)
    {
        Map<String, NodeConfiguration> nodes = new LinkedHashMap<>();
        graph.getNodes().forEach(node -> nodes.putIfAbsent(node.getId(), node));
        Map<String, Integer> incoming = new HashMap<>();
        Map<String, Integer> outgoing = new HashMap<>();
        Map<String, String> next = new HashMap<>();
        Set<String> ports = new HashSet<>();
        for (ConnectionConfiguration connection : graph.getConnections() == null ? List.<ConnectionConfiguration>of() : graph.getConnections()) {
            String source = endpoint(connection.getSource(), nodes, types, "output");
            String target = endpoint(connection.getTarget(), nodes, types, "input");
            if (source == null || target == null || source.equals(target)) {
                errors.add("Connection must join an output port to a different input port");
                continue;
            }
            if (!ports.add(connection.getSource()) || !ports.add(connection.getTarget())) {
                errors.add("Each port accepts a single connection");
                continue;
            }
            outgoing.merge(source, 1, Integer::sum);
            incoming.merge(target, 1, Integer::sum);
            next.put(source, target);
        }
        for (NodeConfiguration node : nodes.values()) {
            int inputs = incoming.getOrDefault(node.getId(), 0);
            int outputs = outgoing.getOrDefault(node.getId(), 0);
            if (inputs != ("source".equals(node.getCategory()) ? 0 : 1) || outputs != ("sink".equals(node.getCategory()) ? 0 : 1)) {
                errors.add(label(node) + ": connect all nodes in a single input-to-output pipeline");
            }
        }
        List<NodeConfiguration> chain = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        String cursor = nodes.values().stream().filter(node -> "source".equals(node.getCategory())).map(NodeConfiguration::getId).findFirst().orElse(null);
        while (cursor != null && visited.add(cursor)) {
            chain.add(nodes.get(cursor));
            cursor = next.get(cursor);
        }
        if (cursor != null || visited.size() != nodes.size()) {
            errors.add("The workflow contains a cycle or disconnected nodes");
        }
        return errors.isEmpty() ? chain : null;
    }

    /** The node owning a port endpoint ({@code <node id>-<port id>}) of the given direction. */
    private String endpoint(String endpoint, Map<String, NodeConfiguration> nodes, Map<String, NodeConfiguration> types, String direction)
    {
        if (endpoint == null) {
            return null;
        }
        for (NodeConfiguration node : nodes.values()) {
            NodeConfiguration type = types.get(node.getTid());
            List<PortConfiguration> declared = type == null || type.getPorts() == null ? List.of() : type.getPorts();
            for (PortConfiguration port : declared) {
                if (direction.equals(port.getType()) && endpoint.equals(node.getId() + "-" + port.getId())) {
                    return node.getId();
                }
            }
        }
        return null;
    }

    /** Checks values against declared fields: required, options, types and numeric bounds. */
    static List<String> fieldErrors(String owner, List<NodeItemConfiguration> fields, Map<String, Object> data)
    {
        List<String> errors = new ArrayList<>();
        Map<String, Object> values = data == null ? Map.of() : data;
        for (NodeItemConfiguration field : fields == null ? List.<NodeItemConfiguration>of() : fields) {
            Object value = values.get(field.getField());
            String name = owner + ": " + (field.getLabel() == null ? field.getField() : field.getLabel());
            boolean empty = value == null || value.toString().isBlank()
                    || value instanceof List && ((List<?>) value).isEmpty()
                    || value instanceof Map && ((Map<?, ?>) value).isEmpty();
            if (empty) {
                if (field.isRequired()) {
                    errors.add(name + " is required");
                }
                continue;
            }
            if (field.getOptions() != null && !field.getOptions().isEmpty()
                    && field.getOptions().stream().noneMatch(option -> Objects.equals(option.getValue(), value))) {
                errors.add(name + " has an invalid value");
            }
            switch (field.getType()) {
                case "number":
                    if (!(value instanceof Number)) {
                        errors.add(name + " must be numeric");
                    }
                    else if (field.getMin() != null && ((Number) value).doubleValue() < field.getMin()
                            || field.getMax() != null && ((Number) value).doubleValue() > field.getMax()) {
                        errors.add(name + " is out of range");
                    }
                    break;
                case "switch":
                    if (!(value instanceof Boolean)) {
                        errors.add(name + " must be boolean");
                    }
                    break;
                case "array":
                    if (!(value instanceof List)) {
                        errors.add(name + " must be a list");
                    }
                    break;
                case "map":
                case "mapping":
                    if (!(value instanceof Map)) {
                        errors.add(name + " must be a map");
                    }
                    break;
                default:
                    break;
            }
        }
        return errors;
    }

    private static String label(NodeConfiguration node)
    {
        return node.getLabel() == null ? node.getTid() : node.getLabel();
    }
}
