package net.sumaris.rdf.core.model;

/*-
 * #%L
 * SUMARiS:: RDF features
 * %%
 * Copyright (C) 2018 - 2020 SUMARiS Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */


import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import fr.eaufrance.sandre.schema.apt.APT;
import fr.eaufrance.sandre.schema.com.COM;
import fr.eaufrance.sandre.schema.inc.INC;
import fr.eaufrance.sandre.schema.par.PAR;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.*;
import org.purl.GR;
import org.schema.SCHEMA;
import org.tdwg.rs.DWC;
import org.w3.GEO;
import org.w3.W3NS;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class ModelURIs {

    public static final Map<String, String> NAMESPACE_BY_PREFIX = ImmutableMap.<String, String>builder()
        .put("rdf", RDF.uri)
        .put("rdfs", RDFS.getURI())
        .put("owl", OWL2.NS)

        .put("dc", DC_11.NS)
        .put("dcam", "http://purl.org/dc/dcam/") // DCMI abstract model
        .put("dcterms", DCTerms.NS) // DCMI Terms
        .put("dctypes", DCTypes.NS) // DCMI Types

        .put("foaf", FOAF.NS)
        .put("skos", SKOS.getURI())
        .put(GR.PREFIX, GR.NS) // Good relation
        .put(W3NS.Org.PREFIX, W3NS.Org.NS)
        .put(SCHEMA.PREFIX, SCHEMA.NS)

        // Spatial
        .put("spatial",org.eclipse.rdf4j.model.vocabulary.GEO.NAMESPACE) // GeoSparql
        .put("geo", "http://www.w3.org/2003/01/geo/wgs84_pos#")
        .put("gn", "http://www.geonames.org/ontology#") // Geo names

        // Darwin core
        .put("dwc", DWC.Terms.URI)
        .put("dwciri", "http://rs.tdwg.org/dwc/iri/")
        .put("dwctax", "http://rs.tdwg.org/ontology/voc/TaxonName#")

        // TaxonConcept
        .put("txn", "http://lod.taxonconcept.org/ontology/txn.owl")

        // Data catalog
        .put("dcat", "http://www.w3.org/ns/dcat")

        // Sandre
        .put(APT.PREFIX, APT.NS)
        .put(INC.PREFIX, INC.NS)
        .put(COM.PREFIX, COM.NS)

        // TaxRef (MNHN)
        .put("taxref", "http://taxref.mnhn.fr/lod/")
        .put("taxrefprop", "http://taxref.mnhn.fr/lod/property/")

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
        .put("spatial", org.eclipse.rdf4j.model.vocabulary.GEO.NAMESPACE)
        .put("geo", GEO.WGS84Pos.NS)
        .put("gn", "http://www.geonames.org/ontology") // Geo names

        .put("foaf", "http://xmlns.com/foaf/spec/index.rdf")
        .put("skos", SKOS.getURI())
        .put(W3NS.Org.PREFIX, W3NS.Org.NS + ".rdf")

        // Data Catalog
        .put("dcat", "http://www.w3.org/ns/dcat")

        // Darwin core
        .put("dwc", "http://rs.tdwg.org/dwc/terms/")
        .put("dwctax", "http://rs.tdwg.org/ontology/voc/TaxonName.rdf")

        // TaxonConcept
        .put("txn", "http://lod.taxonconcept.org/ontology/txn.owl")

        // Sandre
        .put(APT.PREFIX, APT.MODEL_URL)
        .put(INC.PREFIX, INC.MODEL_URL)
        .put(PAR.PREFIX, PAR.MODEL_URL)

        .build();

    public static String getTypeUri(Resource schema, Type type) {
        return getTypeUri(schema.getURI(), type);
    }

    public static String getTypeUri(String schemaUri, Type type) {
        return getTypeUri(schemaUri, type.getTypeName());
    }

    public static String getTypeUri(String schemaUri, String className) {
        className = className.substring(className.lastIndexOf(".") + 1);
        String uri = schemaUri + className;
        if (uri.substring(1).contains("<")) {
            uri = uri.substring(0, uri.indexOf("<"));
        }

        return uri;
    }

    public static String getDataUri(Resource schema, Class clazz) {
        return getDataUri(schema.getURI(), clazz);
    }

    public static String getDataUri(String schemaUri, Class clazz) {
        String uri = getTypeUri(schemaUri, clazz);
        // Replace /schema by /data
        uri = uri.replace("/" + ModelType.SCHEMA.name().toLowerCase(), "/" + ModelType.DATA.name().toLowerCase());

        // Remove version
        uri = uri.replace("/" + ModelType.SCHEMA.name().toLowerCase(), "/" + ModelType.DATA.name().toLowerCase());

        return uri;
    }

    public static Optional<String> getModelUrlByNamespace(String ns) {
        Preconditions.checkNotNull(ns);
        return NAMESPACE_BY_PREFIX.entrySet().stream()
            .filter(entry -> ns.startsWith(entry.getValue()))
            .map(Map.Entry::getKey)
            .map(RDF_URL_BY_PREFIX::get)
            .filter(Objects::nonNull)
            // Keep longest IRI
            .sorted(Comparator.comparingInt(String::length))
            .sorted(Comparator.reverseOrder())
            .findFirst();
    }

    public static List<String> getModelUrlByPrefix(String... prefixes) {
        return Arrays.stream(prefixes)
            .map(RDF_URL_BY_PREFIX::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
