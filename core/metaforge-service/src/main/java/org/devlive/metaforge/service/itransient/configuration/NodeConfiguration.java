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
@JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
@JsonInclude(JsonInclude.Include.NON_NULL)
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC"})
public class NodeConfiguration
{
    private String id;
    private String key;
    private String tid;
    private String label;
    private String category;
    private PositionConfiguration position;
    private List<NodeItemConfiguration> configure;
    private String description;
    private List<PortConfiguration> ports;
    private Map<String, Object> data;
}
