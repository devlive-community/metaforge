package org.devlive.metaforge.common.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j
public class CSVUtils
{
    private final static String NEW_LINE_SEPARATOR = "\n";
    private final static URL PATH = Thread.currentThread().getContextClassLoader().getResource("");

    private CSVUtils()
    {
    }

    /**
     * Generates a temporary CSV file with the given path, file name, header, and values.
     *
     * @param path the directory path where the CSV file will be created
     * @param fileName the name of the CSV file
     * @param head the list of header values for the CSV file
     * @param values the list of values to be written to the CSV file
     * @return the created temporary CSV file
     * @throws IOException if an I/O error occurs while creating or writing to the CSV file
     */
    public static File makeTempCSV(String path, String fileName, List<String> head, List<Object> values)
            throws IOException
    {
        CSVFormat format = CSVFormat.DEFAULT.withRecordSeparator(NEW_LINE_SEPARATOR);
        File directory = new File(path);
        if (!directory.exists()) {
            log.warn("The directory [ {} ] doesn't exist, create it [ {} ]", path, directory.mkdirs());
        }
        File file = File.createTempFile(fileName, null, directory);
        file.deleteOnExit();
        try (FileOutputStream fileOutputStream = new FileOutputStream(file);
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);
                BufferedWriter writer = new BufferedWriter(outputStreamWriter);
                CSVPrinter printer = new CSVPrinter(writer, format)) {
            printer.printRecord(head);
            for (Object value : values) {
                if (value instanceof ObjectNode) {
                    ObjectNode node = (ObjectNode) value;
                    List<Object> newValues = new ArrayList<>();
                    Iterator<JsonNode> valuesIterator = node.elements();
                    while (valuesIterator.hasNext()) {
                        JsonNode jsonNode = valuesIterator.next();
                        newValues.add(jsonNode.asText());
                    }
                    printer.printRecord(newValues);
                }
                else {
                    printer.printRecord(value);
                }
            }
        }
        return file;
    }
}
