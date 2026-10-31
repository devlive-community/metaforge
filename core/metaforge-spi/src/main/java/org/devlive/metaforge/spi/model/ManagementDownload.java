package org.devlive.metaforge.spi.model;

import com.fasterxml.jackson.annotation.JsonView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import org.devlive.metaforge.common.view.EntityView;

@Data
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class ManagementDownload
{
    @JsonView({EntityView.UserView.class, EntityView.AdminView.class})
    private String url;
    @JsonView({EntityView.UserView.class, EntityView.AdminView.class})
    private String fileName;
    @JsonView({EntityView.UserView.class, EntityView.AdminView.class})
    private long expiresAt;
}
