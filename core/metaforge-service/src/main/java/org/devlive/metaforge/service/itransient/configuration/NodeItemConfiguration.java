package org.devlive.metaforge.service.itransient.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.devlive.metaforge.common.view.EntityView;

import java.util.List;
import java.util.Map;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC"})
public class NodeItemConfiguration
{
    private String field;
    private String label;
    private String type;
    private String description;
    private boolean required;
    private String placeholder;
    private Object value;
    private List<RuleConfiguration> rules;
    private List<OptionConfiguration> options;
    private Double min;
    private Double max;
    private Double step;
    /** Editor tab of the field: basic, sql or advanced. Derived from the field when absent. */
    private String group;
    /**
     * Fields of the same node this field reads its choices from, such as {@code {source: source}} for
     * a database list or {@code {source: source, database: database, table: table}} for target columns.
     * Changing one of them clears this field.
     */
    private Map<String, String> from;
    /** Fields of the upstream node supplying incoming columns, such as {@code {source: source, query: query}}. */
    private Map<String, String> upstream;
}
