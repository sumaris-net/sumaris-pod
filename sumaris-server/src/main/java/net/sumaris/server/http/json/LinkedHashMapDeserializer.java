package net.sumaris.server.http.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public class LinkedHashMapDeserializer extends JsonDeserializer<Object> {

    @Override
    public Object deserialize(JsonParser jp, DeserializationContext context)
            throws IOException {
        Map map = new LinkedHashMap();

        ObjectCodec oc = jp.getCodec();
        ObjectNode mapNode = oc.readTree(jp);
        Iterator it = mapNode.elements();
        while(it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next();
            map.put(entry.getKey(), entry.getValue());
        }

        return map;
    }
}
