package org.devlive.metaforge.service.workflow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import org.devlive.metaforge.service.itransient.configuration.NodeConfiguration;
import org.devlive.metaforge.service.itransient.configuration.NodeItemConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What an executor plugin declares about workflows in its {@code workflow.yaml}: how runs can be
 * observed and stopped, which graph shapes it executes, the job settings a workflow may set and the
 * node plugins it offers. The editor and the server both follow this declaration.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class WorkflowExecutorDefinition
{
    public static final String PIPELINE = "pipeline";
    public static final List<String> CATEGORIES = List.of("source", "transform", "sink");

    private String name;
    private String description;
    /** Graph shape the executor runs; only a single input-to-output {@value #PIPELINE} for now. */
    private String topology = PIPELINE;
    private Capabilities capabilities = new Capabilities();
    /** Maximum number of nodes per category, keyed by {@code sources}, {@code transforms} and {@code sinks}. */
    private Map<String, Integer> limits = new LinkedHashMap<>();
    private List<NodeItemConfiguration> settings = new ArrayList<>();
    /** Relative node file paths in the declaration; resolved node definitions once read. */
    private List<NodeConfiguration> nodes = new ArrayList<>();

    public List<String> getCategories()
    {
        return CATEGORIES;
    }

    public int limit(String category)
    {
        Integer value = limits.get(category + "s");
        return value == null ? ("transform".equals(category) ? 0 : 1) : Math.max(0, value);
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Capabilities
    {
        public static final String STOP_COOPERATIVE = "cooperative";
        public static final String STOP_INTERRUPT = "interrupt";
        public static final String STOP_NONE = "none";

        /** {@value #STOP_COOPERATIVE}, {@value #STOP_INTERRUPT} or {@value #STOP_NONE}. */
        private String stop = STOP_INTERRUPT;
        /** The executor reports how many rows it wrote; otherwise the count stays unknown. */
        private boolean rowCount;
        private boolean metrics;
        /** The executor returns a bounded sample of written rows. */
        private boolean preview;

        public boolean cooperativeStop()
        {
            return STOP_COOPERATIVE.equals(stop);
        }

        public boolean stoppable()
        {
            return !STOP_NONE.equals(stop);
        }
    }
}
