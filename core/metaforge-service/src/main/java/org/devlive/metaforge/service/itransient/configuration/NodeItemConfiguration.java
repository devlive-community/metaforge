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
}
