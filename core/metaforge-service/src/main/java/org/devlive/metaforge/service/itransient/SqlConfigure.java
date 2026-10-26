package org.devlive.metaforge.service.itransient;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.devlive.metaforge.common.enums.Type;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SqlConfigure
{
    private String column;
    private Type type;
    private String expression;
}
