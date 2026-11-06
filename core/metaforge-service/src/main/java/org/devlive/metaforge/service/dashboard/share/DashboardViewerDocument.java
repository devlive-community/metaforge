package org.devlive.metaforge.service.dashboard.share;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * What a viewer of a shared data screen receives: only the published design, never the draft, and
 * when the owner chose widgets only those widgets and the widgets inside them. Everything else,
 * including the reports behind hidden widgets, stays on the server.
 */
public final class DashboardViewerDocument
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private DashboardViewerDocument()
    {}

    /**
     * The viewer's document: version 3 whose draft and published both hold the visible published
     * design. Empty when nothing is published or the document cannot be read.
     *
     * @param widgets widget ids to show, or null for the whole screen
     */
    public static Optional<String> of(String configure, Set<String> widgets)
    {
        Optional<JsonNode> root = parse(configure);
        if (root.isEmpty()) {
            return Optional.empty();
        }
        try {
            if (root.get().isArray()) {
                // Earlier layouts are a widget array, published once saved; ids follow the editor's legacy-<index>.
                ArrayNode visible = MAPPER.createArrayNode();
                for (int index = 0; index < root.get().size(); index++) {
                    if (widgets == null || widgets.contains("legacy-" + index)) {
                        visible.add(root.get().get(index));
                    }
                }
                return Optional.of(MAPPER.writeValueAsString(visible));
            }
            JsonNode published = root.get().path("version").asInt() == 3 ? root.get().path("published") : null;
            if (published == null || !published.isObject()) {
                return Optional.empty();
            }
            ObjectNode design = published.deepCopy();
            if (widgets != null) {
                JsonNode boards = design.path("boards");
                if (boards.isObject()) {
                    boards.fields().forEachRemaining(entry -> filter(entry.getValue(), widgets));
                }
            }
            ObjectNode document = MAPPER.createObjectNode();
            document.put("version", 3);
            document.set("draft", design);
            document.set("published", design.deepCopy());
            return Optional.of(MAPPER.writeValueAsString(document));
        }
        catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    /** Report codes the widgets of a (viewer) document draw. */
    public static Set<String> reportCodes(String configure)
    {
        Set<String> codes = new LinkedHashSet<>();
        Optional<JsonNode> root = parse(configure);
        if (root.isEmpty()) {
            return codes;
        }
        if (root.get().isArray()) {
            for (JsonNode item : root.get()) {
                String code = item.path("original").path("code").asText(item.path("node").path("id").asText(""));
                if (!code.isEmpty()) {
                    codes.add(code);
                }
            }
            return codes;
        }
        for (String part : new String[] {"draft", "published"}) {
            JsonNode boards = root.get().path(part).path("boards");
            if (boards.isObject()) {
                boards.elements().forEachRemaining(board -> board.path("widgets").forEach(widget -> {
                    String code = widget.path("reportCode").asText("");
                    if (!code.isEmpty()) {
                        codes.add(code);
                    }
                }));
            }
        }
        return codes;
    }

    /** Widget ids of the published design, for the owner's widget picker. */
    public static Set<String> publishedWidgetIds(String configure)
    {
        Set<String> ids = new LinkedHashSet<>();
        parse(configure).map(root -> root.path("published").path("boards")).filter(JsonNode::isObject)
                .ifPresent(boards -> boards.elements().forEachRemaining(board -> board.path("widgets")
                        .forEach(widget -> ids.add(widget.path("id").asText("")))));
        ids.remove("");
        return ids;
    }

    /** Keeps the chosen widgets and everything inside them (containers hold children by parentId). */
    private static void filter(JsonNode board, Set<String> widgets)
    {
        JsonNode list = board.path("widgets");
        if (!list.isArray()) {
            return;
        }
        Map<String, String> parents = new HashMap<>();
        list.forEach(widget -> parents.put(widget.path("id").asText(""), widget.path("parentId").asText("")));
        ArrayNode kept = MAPPER.createArrayNode();
        for (JsonNode widget : list) {
            if (visible(widget.path("id").asText(""), widgets, parents)) {
                kept.add(widget);
            }
        }
        ((ObjectNode) board).set("widgets", kept);
    }

    private static boolean visible(String id, Set<String> widgets, Map<String, String> parents)
    {
        Set<String> seen = new HashSet<>();
        for (String current = id; !current.isEmpty() && seen.add(current); current = parents.getOrDefault(current, "")) {
            if (widgets.contains(current)) {
                return true;
            }
        }
        return false;
    }

    private static Optional<JsonNode> parse(String configure)
    {
        if (configure == null || configure.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode root = MAPPER.readTree(configure);
            return root == null || root.isNull() || root.isMissingNode() ? Optional.empty() : Optional.of(root);
        }
        catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }
}
