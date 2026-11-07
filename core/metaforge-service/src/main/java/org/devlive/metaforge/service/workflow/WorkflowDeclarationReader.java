package org.devlive.metaforge.service.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.devlive.metaforge.service.itransient.configuration.NodeConfiguration;
import org.devlive.metaforge.service.itransient.configuration.NodeItemConfiguration;
import org.devlive.metaforge.service.itransient.configuration.PortConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reads an executor's {@code workflow.yaml}. Node entries are paths relative to the declaration
 * (or inline node objects), so each node plugin keeps its own file next to the declaration.
 */
public final class WorkflowDeclarationReader
{
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    private WorkflowDeclarationReader()
    {}

    public static WorkflowExecutorDefinition read(String executor, URL declaration)
            throws IOException
    {
        JsonNode root = tree(declaration);
        if (!(root instanceof ObjectNode)) {
            throw new IOException(declaration + " is not a workflow declaration");
        }
        List<NodeConfiguration> nodes = new ArrayList<>();
        for (JsonNode entry : root.path("nodes")) {
            NodeConfiguration node = entry.isTextual()
                    ? YAML.treeToValue(tree(new URL(declaration, entry.asText())), NodeConfiguration.class)
                    : YAML.treeToValue(entry, NodeConfiguration.class);
            nodes.add(check(declaration, node));
        }
        ((ObjectNode) root).remove("nodes");
        WorkflowExecutorDefinition definition = YAML.treeToValue(root, WorkflowExecutorDefinition.class);
        definition.setName(executor);
        definition.setNodes(nodes);
        Set<String> ids = new HashSet<>();
        for (NodeConfiguration node : nodes) {
            if (!ids.add(node.getId())) {
                throw new IOException(declaration + " declares the node " + node.getId() + " twice");
            }
        }
        return definition;
    }

    private static NodeConfiguration check(URL declaration, NodeConfiguration node)
            throws IOException
    {
        if (node.getId() == null || node.getKey() == null || !WorkflowExecutorDefinition.CATEGORIES.contains(node.getCategory())) {
            throw new IOException(declaration + " declares a node without id, key or a known category: " + node.getId());
        }
        if (node.getTid() == null) {
            node.setTid(node.getId());
        }
        if (node.getConfigure() == null) {
            node.setConfigure(new ArrayList<>());
        }
        if (node.getPorts() == null || node.getPorts().isEmpty()) {
            // Ports follow from the category: inputs read, outputs write, transforms do both.
            List<PortConfiguration> ports = new ArrayList<>();
            if (!"source".equals(node.getCategory())) {
                ports.add(new PortConfiguration("input", "input", "Input", true, null));
            }
            if (!"sink".equals(node.getCategory())) {
                ports.add(new PortConfiguration("output", "output", "Output", true, null));
            }
            node.setPorts(ports);
        }
        for (NodeItemConfiguration field : node.getConfigure()) {
            if (field.getField() == null || field.getType() == null) {
                throw new IOException(declaration + " node " + node.getId() + " has a field without name or type");
            }
        }
        return node;
    }

    private static JsonNode tree(URL url)
            throws IOException
    {
        URLConnection connection = url.openConnection();
        // Plugins are reloaded from their jars; a cached jar would stay open and stale.
        connection.setUseCaches(false);
        try (InputStream stream = connection.getInputStream()) {
            return YAML.readTree(stream);
        }
    }
}
