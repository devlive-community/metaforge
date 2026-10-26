package org.devlive.metaforge.service.body;

import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.devlive.metaforge.common.sql.configure.SqlColumn;

import java.util.List;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class ColumnFilter
{
    private String condition = "AND";
    private List<SqlColumn> filters = Lists.newArrayList();
}
