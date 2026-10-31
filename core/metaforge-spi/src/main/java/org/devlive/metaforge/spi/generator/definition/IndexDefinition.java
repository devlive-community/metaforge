package org.devlive.metaforge.spi.generator.definition;

import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "Definition holders intentionally expose mutable lists to the DDL builders")
public class IndexDefinition
{
    private String name;
    private List<String> columns = Lists.newArrayList();
    private boolean unique;
    private String comment;
    // BTREE, HASH, FULLTEXT or SPATIAL; empty keeps the engine default.
    private String type;
    // ALTER algorithm: INPLACE or COPY; empty lets the server choose.
    private String algorithm;
    // Per-column sort order (ASC or DESC) and prefix length, aligned with columns; missing entries use the defaults.
    private List<String> orders = Lists.newArrayList();
    private List<Integer> lengths = Lists.newArrayList();
    // The index being edited or rebuilt; it is dropped and recreated in the same statement.
    private String originalName;
}
