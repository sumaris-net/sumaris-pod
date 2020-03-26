package net.sumaris.rdf.model;


import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import fr.eaufrance.sandre.schema.apt.APT;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.*;
import org.tdwg.rs.DWC;

import java.util.Map;

public class ModelURIs {

    public static final Map<String, String> NAMESPACE_BY_PREFIX = ImmutableMap.<String, String>builder()
            .put("rdf", RDF.uri)
            .put("rdfs", RDFS.getURI())
            .put("owl", OWL2.NS)

            .put("dc", DC_11.NS)
            .put("dcam", "http://purl.org/dc/dcam/") // DCMI abstract model
            .put("dcterms", DCTerms.NS) // DCMI Terms
            .put("dctypes", DCTypes.NS) // DCMI Types

            // Spatial
            .put("spatial", "http://www.opengis.net/ont/geosparql#") // GeoSparql
            .put("geo", "http://www.w3.org/2003/01/geo/wgs84_pos#")
            .put("gn", "http://www.geonames.org/ontology#") // Geo names

            .put("foaf", FOAF.NS)
            .put("skos", SKOS.getURI())

            // Darwin core
            .put("dwc", DWC.Terms.NS)

            // Sandre
            .put("apt", APT.NS)
            .build();

    public static final Map<String, String> RDF_URL_BY_PREFIX = ImmutableMap.<String, String>builder()
            .put("rdf",  RDF.uri)
            .put("rdfs", RDFS.getURI())
            .put("owl",  OWL2.NS)

            .put("dc", "https://www.dublincore.org/specifications/dublin-core/dcmi-terms/dublin_core_elements.rdf")
            .put("dcam", "https://www.dublincore.org/specifications/dublin-core/dcmi-terms/dublin_core_abstract_model.rdf")
            .put("dcterms", "https://www.dublincore.org/specifications/dublin-core/dcmi-terms/dublin_core_terms.rdf")
            .put("dctypes", "https://www.dublincore.org/specifications/dublin-core/dcmi-terms/dublin_core_type.rdf")

            // TODO: find the prefix
            .put("spatial", "http://www.opengis.net/ont/geosparql")
            .put("geo", "http://www.w3.org/2003/01/geo/wgs84_pos")
            .put("gn", "http://www.geonames.org/ontology") // Geo names

            //.put("", "http://www.w3.org/2000/10/swap/pim/contact")
            .put("foaf", "http://xmlns.com/foaf/spec/index.rdf")
            .put("skos", SKOS.getURI())

            .put("dwc", "http://rs.tdwg.org/ontology/voc/TaxonName.rdf+")

            // Sandre
            .put("apt", "http://owl.sandre.eaufrance.fr/apt/2.1/sandre_fmt_owl_apt.owl")
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

    public static String getClassUri(String schemaUri, String simpleClassName) {
        String uri = schemaUri + simpleClassName;
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

    public static String getClassNameFromUri(String classUri) {
        Preconditions.checkNotNull(classUri);

        int separatorIndex = classUri.lastIndexOf('#');
        if (separatorIndex == -1) {
            separatorIndex = classUri.lastIndexOf('/');
        }
        if (separatorIndex == -1 || separatorIndex == classUri.length()-1) {
            throw new IllegalArgumentException("Unable to parse class uri: " + classUri);
        }
        return  classUri.substring(separatorIndex+1);
    }
}
