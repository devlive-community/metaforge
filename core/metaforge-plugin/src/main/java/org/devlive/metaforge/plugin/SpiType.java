package org.devlive.metaforge.plugin;

import lombok.Getter;

@Getter
public enum SpiType
{
    COMPILED_POM("CompiledPom"),
    DIRECTORY("Directory"),
    POM("Pom"),
    PROPERTIES("Properties"),
    SPI("Spi"),
    INJECT("Inject"),
    TAR("Tar");

    private final String name;

    SpiType(String name)
    {
        this.name = name;
    }

    public static SpiType fromName(String name)
    {
        return valueOf(name);
    }
}
