package org.devlive.metaforge.condor.metadata;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import org.devlive.metaforge.condor.manager.TableManager;

import java.nio.file.Path;

@Getter
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class DatabaseDefinition
{
    private String name;
    private Path path;
    private TableManager tableManager;

    public DatabaseDefinition(String name, Path path)
    {
        this.name = name;
        this.path = path;
        this.tableManager = new TableManager(path);
    }
}
