package org.devlive.metaforge.service.enums;

public enum ScheduledType
{
    SOURCE_CHECK, // Check that the data source is available
    SOURCE_SYNCHRONIZE, // Synchronize table structure
    SOURCE_SYNC_METADATA
}
