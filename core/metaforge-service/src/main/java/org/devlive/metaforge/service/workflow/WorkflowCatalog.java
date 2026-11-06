package org.devlive.metaforge.service.workflow;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.service.entity.WorkflowEntity;
import org.devlive.metaforge.service.itransient.configuration.Configuration;
import org.devlive.metaforge.service.itransient.configuration.NodeConfiguration;
import org.devlive.metaforge.service.itransient.configuration.NodeItemConfiguration;
import org.devlive.metaforge.service.service.ConfigureService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** The server is the authority for the node palette and executable graph shapes. */
@Component
public class WorkflowCatalog
{
    private final ConfigureService configureService;
    private final PluginManager plugins;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The plugin manager is an intentionally shared Spring dependency")
    public WorkflowCatalog(ConfigureService configureService, PluginManager plugins)
    {
        this.configureService = configureService;
        this.plugins = plugins;
    }

    public Map<String, Object> catalog()
    {
        CommonResponse<Configuration> response = configureService.getExecutor();
        if (!Boolean.TRUE.equals(response.getStatus())) {
            throw new IllegalArgumentException(String.valueOf(response.getMessage()));
        }
        List<Map<String, Object>> executors = new ArrayList<>();
        for (String name : List.of("MetaForge", "Seatunnel")) {
            if (plugins.getPlugin(name).isEmpty()) {
                continue;
            }
            List<NodeConfiguration> nodes = response.getData().getNodes().stream()
                    // Older node files describe SeaTunnel connectors only.
                    .filter(node -> node.getSupportExecutors() == null
                            ? name.equals("Seatunnel") : node.getSupportExecutors().contains(name))
                    .collect(Collectors.toList());
            Map<String, Object> executor = new LinkedHashMap<>();
            executor.put("name", name);
            executor.put("nodes", nodes);
            executor.put("maxTransforms", name.equals("MetaForge") ? 0 : 1);
            executor.put("categories", List.of("source", "transform", "sink"));
            executors.add(executor);
        }
        return Map.of("executors", executors);
    }

    @SuppressWarnings("unchecked")
    public List<NodeConfiguration> definitions(String executor)
    {
        List<Map<String, Object>> items = (List<Map<String, Object>>) catalog().get("executors");
        return items.stream().filter(item -> Objects.equals(item.get("name"), executor))
                .findFirst().map(item -> (List<NodeConfiguration>) item.get("nodes"))
                .orElseThrow(() -> new IllegalArgumentException("Executor is unavailable: " + executor));
    }

    public List<String> validate(WorkflowEntity workflow)
    {
        List<String> errors = new ArrayList<>();
        try {
            List<NodeConfiguration> definitions = definitions(workflow.getExecutor());
            Configuration graph = workflow.getConfigure();
            if (graph == null || graph.getNodes() == null || graph.getNodes().isEmpty()) {
                return List.of("Add an input and an output node");
            }
            if (graph.getNodes().size() > 100 || (graph.getConnections() != null && graph.getConnections().size() > 200)) {
                return List.of("Workflow is too large");
            }
            if (graph.getTimeout() != null && (graph.getTimeout() < 1 || graph.getTimeout() > 86400)) {
                errors.add("Timeout must be between 1 and 86400 seconds");
            }
            Map<String, NodeConfiguration> allowed = definitions.stream().collect(Collectors.toMap(NodeConfiguration::getId, node -> node));
            Map<String, NodeConfiguration> nodes = new LinkedHashMap<>();
            Map<String, Integer> incoming = new HashMap<>();
            Map<String, Integer> outgoing = new HashMap<>();
            Map<String, String> next = new HashMap<>();
            for (NodeConfiguration node : graph.getNodes()) {
                if (node.getId() == null || nodes.putIfAbsent(node.getId(), node) != null) {
                    errors.add("Node identifiers must be present and unique");
                }
                NodeConfiguration definition = allowed.get(node.getTid());
                if (definition == null || !Objects.equals(definition.getCategory(), node.getCategory())
                        || !Objects.equals(definition.getKey(), node.getKey())) {
                    errors.add("Unsupported node for " + workflow.getExecutor() + ": " + node.getTid());
                    continue;
                }
                Map<String, Object> values = node.getData() == null ? Map.of() : node.getData();
                for (NodeItemConfiguration field : definition.getConfigure()) {
                    Object value = values.get(field.getField());
                    if (field.isRequired() && (value == null || value.toString().isBlank()
                            || value instanceof List && ((List<?>) value).isEmpty()
                            || value instanceof Map && ((Map<?, ?>) value).isEmpty())) {
                        errors.add(label(node) + ": " + field.getLabel() + " is required");
                    }
                    if (value != null && field.getOptions() != null && !field.getOptions().isEmpty()
                            && field.getOptions().stream().noneMatch(option -> Objects.equals(option.getValue(), value))) {
                        errors.add(label(node) + ": invalid " + field.getLabel());
                    }
                    if (value != null && "number".equals(field.getType()) && !(value instanceof Number)) {
                        errors.add(label(node) + ": " + field.getLabel() + " must be numeric");
                    }
                    if (value != null && "switch".equals(field.getType()) && !(value instanceof Boolean)) {
                        errors.add(label(node) + ": " + field.getLabel() + " must be boolean");
                    }
                    if (value != null && "array".equals(field.getType()) && !(value instanceof List)) {
                        errors.add(label(node) + ": " + field.getLabel() + " must be a list");
                    }
                    if (value != null && "map".equals(field.getType()) && !(value instanceof Map)) {
                        errors.add(label(node) + ": " + field.getLabel() + " must be a map");
                    }
                }
            }
            for (var connection : graph.getConnections() == null ? List.<org.devlive.metaforge.service.itransient.configuration.ConnectionConfiguration>of() : graph.getConnections()) {
                String source = endpoint(connection.getSource(), nodes, true);
                String target = endpoint(connection.getTarget(), nodes, false);
                if (source == null || target == null || source.equals(target)) {
                    errors.add("Connection must join an output port to a different input port");
                    continue;
                }
                outgoing.merge(source, 1, Integer::sum);
                incoming.merge(target, 1, Integer::sum);
                next.put(source, target);
            }
            List<NodeConfiguration> sources = graph.getNodes().stream().filter(node -> "source".equals(node.getCategory())).collect(Collectors.toList());
            long sinks = graph.getNodes().stream().filter(node -> "sink".equals(node.getCategory())).count();
            long transforms = graph.getNodes().stream().filter(node -> "transform".equals(node.getCategory())).count();
            if (sources.size() != 1 || sinks != 1) {
                errors.add("This executor requires exactly one input and one output");
            }
            if (transforms > ("MetaForge".equals(workflow.getExecutor()) ? 0 : 1)) {
                errors.add("This executor does not support this number of transforms");
            }
            for (NodeConfiguration node : graph.getNodes()) {
                int inputs = incoming.getOrDefault(node.getId(), 0);
                int outputs = outgoing.getOrDefault(node.getId(), 0);
                if (inputs != ("source".equals(node.getCategory()) ? 0 : 1)
                        || outputs != ("sink".equals(node.getCategory()) ? 0 : 1)) {
                    errors.add(label(node) + ": connect all nodes in a single input-to-output pipeline");
                }
            }
            Set<String> visited = new HashSet<>();
            String cursor = sources.isEmpty() ? null : sources.get(0).getId();
            while (cursor != null && visited.add(cursor)) {
                cursor = next.get(cursor);
            }
            if (cursor != null || visited.size() != nodes.size()) {
                errors.add("The workflow contains a cycle or disconnected nodes");
            }
        }
        catch (IllegalArgumentException e) {
            errors.add(e.getMessage());
        }
        return errors;
    }

    private String endpoint(String endpoint, Map<String, NodeConfiguration> nodes, boolean output)
    {
        return nodes.values().stream()
                .filter(node -> Objects.equals(endpoint, node.getId() + (output ? "-output" : "-input")))
                .filter(node -> output ? !"sink".equals(node.getCategory()) : !"source".equals(node.getCategory()))
                .map(NodeConfiguration::getId).findFirst().orElse(null);
    }

    private String label(NodeConfiguration node)
    {
        return node.getLabel() == null ? node.getTid() : node.getLabel();
    }
}
