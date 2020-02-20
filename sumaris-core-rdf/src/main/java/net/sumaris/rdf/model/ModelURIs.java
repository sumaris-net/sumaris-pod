package net.sumaris.rdf.model;


import com.google.common.collect.ImmutableMap;
import org.apache.jena.rdf.model.Resource;

import java.util.Map;

public class ModelURIs {

    public static final Map<String, String> URI_BY_NAMESPACE = ImmutableMap.<String, String>builder()
            .put("foaf", "http://xmlns.com/foaf/spec/index.rdf")
            .put("rdf", "https://www.w3.org/1999/02/22-rdf-syntax-ns")
            .put("rdfs", "http://www.w3.org/2000/01/rdf-schema")
            .put("owl", "http://www.w3.org/2002/07/owl")
            .put("skos", "http://www.w3.org/2004/02/skos/core")
            .build();

    public static String getClassUri(Resource schema, Class clazz) {
        return getClassUri(schema.getURI(), clazz);
    }

    public static String getClassUri(String schemaUri, Class clazz) {
        String uri = schemaUri + clazz.getSimpleName();
        if (uri.substring(1).contains("<")) {
            uri = uri.substring(0, uri.indexOf("<"));
        }

        return uri;
    }

    public static String getBeanUri(Resource schema, Class clazz) {
        return getBeanUri(schema.getURI(), clazz);
    }

    public static String getBeanUri(String schemaUri, Class clazz) {
        String uri = getClassUri(schemaUri, clazz);
        // Replace /schema by /data
        uri = uri.replace("/" + ModelType.SCHEMA.name().toLowerCase(), "/" + ModelType.DATA.name().toLowerCase());

        return uri;
    }
}
