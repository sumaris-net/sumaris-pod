package net.sumaris.rdf.model;


import org.apache.jena.rdf.model.Resource;

public class ModelURIs {

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
