package org.devlive.metaforge.service.configure;

import com.fasterxml.jackson.annotation.JsonView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.devlive.metaforge.common.view.EntityView;

import java.util.Date;
import java.util.List;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2", "EI_EXPOSE_REP2", "EI_EXPOSE_REP", "NP_NULL_PARAM_DEREF_NONVIRTUAL"},
        justification = "I prefer to suppress these FindBugs warnings")
public class IConfigure
{
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String name;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Date supportTime;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private List<IConfigureField> configures;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private List<IConfigureExecutor> pipelines;
}
