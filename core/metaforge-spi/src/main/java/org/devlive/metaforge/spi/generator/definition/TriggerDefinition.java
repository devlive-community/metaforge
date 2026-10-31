package org.devlive.metaforge.spi.generator.definition;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
public class TriggerDefinition
{
    private String name;
    // BEFORE or AFTER
    private String timing;
    // INSERT, UPDATE or DELETE
    private String event;
    // The statement run for each row, usually BEGIN ... END.
    private String body;
    // user@host; empty means the current user.
    private String definer;
    // The trigger being edited or rebuilt; it is dropped and created again.
    private String originalName;
}
