package org.devlive.metaforge.spi.generator.definition;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** Table options to change; a null field is left as it is. */
@Data
@ToString
@NoArgsConstructor
public class TableOptionsDefinition
{
    private String engine;
    private String rowFormat;
    private String charset;
    private String collate;
    private Long autoIncrement;
    private String comment;
}
