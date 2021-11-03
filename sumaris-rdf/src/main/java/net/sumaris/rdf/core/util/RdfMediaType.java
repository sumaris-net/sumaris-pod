/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.rdf.core.util;

import org.springframework.http.MediaType;

public class RdfMediaType extends MediaType {

    protected static RdfMediaType fromString(String contentType) {
        String[] parts = contentType.split("/");
        if (parts.length == 2) {
            return new RdfMediaType(parts[0], parts[1]);
        }
        return new RdfMediaType(contentType);
    }

    public static final String APPLICATION_RDF_XML_VALUE = "application/rdf+xml";
    public static final RdfMediaType APPLICATION_RDF_XML = fromString(APPLICATION_RDF_XML_VALUE);

    public static final String TEXT_N3_VALUE = "text/rdf+n3";
    public static final RdfMediaType TEXT_N3 = fromString(TEXT_N3_VALUE);

    public static final String APPLICATION_N_TRIPLES_VALUE = "application/n-triples";
    public static final RdfMediaType APPLICATION_N_TRIPLES = fromString(APPLICATION_N_TRIPLES_VALUE);

    public static final String APPLICATION_N_QUADS_VALUE = "application/n-quads";
    public static final RdfMediaType APPLICATION_N_QUADS = fromString(APPLICATION_N_QUADS_VALUE);

    public static final String APPLICATION_RDF_JSON_VALUE = "application/rdf+json";
    public static final RdfMediaType APPLICATION_RDF_JSON = fromString(APPLICATION_RDF_JSON_VALUE);


    public static final String APPLICATION_JSON_LD_VALUE = "application/ld+json";
    public static final RdfMediaType APPLICATION_JSON_LD = fromString(APPLICATION_JSON_LD_VALUE);

    public static final String APPLICATION_TURTLE_VALUE = "application/ttl";
    public static final String TEXT_TURTLE_VALUE = "text/turtle";
    public static final RdfMediaType TEXT_TURTLE = fromString(TEXT_TURTLE_VALUE);

    public static final String APPLICATION_TRIG_VALUE = "application/trig";
    public static final String TEXT_TRIG_VALUE = "text/trig";
    public static final RdfMediaType TEXT_TRIG = fromString(TEXT_TRIG_VALUE);

    public static final String APPLICATION_TRIX_VALUE = "application/trix";
    public static final String TEXT_TRIX_VALUE = "text/trix";
    public static final RdfMediaType TEXT_TRIX = fromString(TEXT_TRIX_VALUE);

    public static final String APPLICATION_X_JAVASCRIPT_VALUE = "application/x-javascript";
    public static final RdfMediaType APPLICATION_X_JAVASCRIPT = fromString(APPLICATION_X_JAVASCRIPT_VALUE);

    public static final String APPLICATION_THRIFT_VALUE = "application/x-thrift";
    public static final RdfMediaType APPLICATION_THRIFT = fromString(APPLICATION_THRIFT_VALUE);

    public static final String APPLICATION_RDF_THRIFT_VALUE = "application/rdf+thrift";
    public static final RdfMediaType APPLICATION_RDF_THRIFT = fromString(APPLICATION_RDF_THRIFT_VALUE);

    public static final String APPLICATION_WEBVOWL_VALUE = "application/webvowl+json";
    public static final RdfMediaType APPLICATION_WEBVOWL = fromString(APPLICATION_WEBVOWL_VALUE);


    protected RdfMediaType(String type) {
        super(type);
    }

    protected RdfMediaType(String type, String subType) {
        super(type, subType);
    }

}
