package org.devlive.metaforge.client.cli.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DataSource
{
    private int id;
    private String name;
    private String type;
    private String description;
}
