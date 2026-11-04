package org.devlive.metaforge.server.controller;

import org.devlive.metaforge.service.service.ReportShareService;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PublicReportControllerTest
{
    @Test
    void csvQuotesSpecialCellsAndNeutralisesFormulas()
    {
        ReportShareService.Table table = new ReportShareService.Table(List.of("name", "note", "amount"), List.of(
                Arrays.asList("East", "=HYPERLINK(\"http://x\")", 12),
                Arrays.asList("We,st", "line\nbreak", -3),
                Arrays.asList(null, "@cmd", 1.5)), false);
        String csv = new String(PublicReportController.csv(table), StandardCharsets.UTF_8);
        assertEquals("﻿name,note,amount\r\n"
                + "East,\"'=HYPERLINK(\"\"http://x\"\")\",12\r\n"
                + "\"We,st\",\"line\nbreak\",-3\r\n"
                + ",'@cmd,1.5\r\n", csv);
    }
}
