package org.devlive.metaforge.service.body;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class OrderBody
{
    private String column;
    private String order;
}
