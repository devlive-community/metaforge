package org.devlive.metaforge.spi.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JSON
{
    public static final ObjectMapper objectmapper = new ObjectMapper();

    private JSON() {}

    public static String toJSON(Object object)
    {
        String json;
        try {
            json = objectmapper.writeValueAsString(object);
        }
        catch (JsonProcessingException e) {
            json = null;
        }
        return json;
    }
}
