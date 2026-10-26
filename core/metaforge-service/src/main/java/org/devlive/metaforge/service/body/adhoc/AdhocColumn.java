package org.devlive.metaforge.service.body.adhoc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.devlive.metaforge.service.enums.ColumnMode;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class AdhocColumn
{
    private Long id;
    private String expression;
    private String function;
    private String alias;
    private String order;
    private ColumnMode mode;
    private String value;
}
