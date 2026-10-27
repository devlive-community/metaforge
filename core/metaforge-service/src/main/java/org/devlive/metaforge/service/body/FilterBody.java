package org.devlive.metaforge.service.body;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class FilterBody
{
    private Integer page = 1;
    private Integer size = 10;
    private List<OrderBody> orders;
    private String keyword;
    private Boolean active;
    private String code;
    private String createdFrom;
    private String createdTo;
    private Map<String, FilterCriterionBody> conditions;

    public FilterBody(Integer page, Integer size, List<OrderBody> orders)
    {
        this.page = page;
        this.size = size;
        this.orders = orders;
    }
}
