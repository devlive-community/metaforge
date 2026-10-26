package org.devlive.metaforge.service.itransient;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ContributionHistory
{
    private String date;
    private Long count;
    private Integer month;
    private String week;
    private Integer day;
    private Boolean lastDay;
    private Boolean lastWeek;
}
