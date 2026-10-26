package org.devlive.metaforge.spi.generator.definition;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.devlive.metaforge.spi.generator.DataType;
import org.devlive.metaforge.spi.generator.Filter;
import org.devlive.metaforge.spi.generator.OrderBy;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class ColumnDefinition
        extends BaseDefinition
{
    private String database;
    private String table;
    private String name;
    private DataType type;
    private Integer length;
    private Integer precision;
    private Integer scale;
    private boolean nullable = true;
    private String defaultValue;
    private String comment;
    private String charset;
    private String collate;
    private boolean primaryKey;
    private boolean autoIncrement;
    private OrderBy.Direction order;
    private Filter.Operator operator;
    private Object value;
    private JsonNode original; // 原始数据，删除需要依赖该数据判断
}
