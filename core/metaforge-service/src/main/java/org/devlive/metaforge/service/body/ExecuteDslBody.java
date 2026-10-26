package org.devlive.metaforge.service.body;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import lombok.ToString;
import org.devlive.metaforge.common.sql.configure.SqlBody;

import java.util.Map;

@Data
@ToString
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class ExecuteDslBody
{
    private String name;
    private SqlBody configure;
    private Map<String, Object> env;
    private String format = "";
}
