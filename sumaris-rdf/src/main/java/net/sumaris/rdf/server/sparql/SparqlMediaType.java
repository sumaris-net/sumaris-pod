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

package net.sumaris.rdf.server.sparql;

import org.springframework.http.MediaType;

public class SparqlMediaType extends MediaType {

    protected static SparqlMediaType fromString(String contentType) {
        String[] parts = contentType.split("/");
        if (parts.length == 2) {
            return new SparqlMediaType(parts[0], parts[1]);
        }
        return new SparqlMediaType(contentType);
    }

    public static final String APPLICATION_SPARQL_RESULT_XML_VALUE = "application/sparql-results+xml";
    public static final SparqlMediaType APPLICATION_SPARQL_RESULT_XML = fromString(APPLICATION_SPARQL_RESULT_XML_VALUE);

    public static final String APPLICATION_SPARQL_RESULT_JSON_VALUE = "application/sparql-results+json";
    public static final SparqlMediaType APPLICATION_SPARQL_RESULT_JSON = fromString(APPLICATION_SPARQL_RESULT_JSON_VALUE);

    public static final String APPLICATION_SPARQL_RESULT_CSV_VALUE = "application/sparql-results+csv'";
    public static final SparqlMediaType APPLICATION_SPARQL_RESULT_CSV = fromString(APPLICATION_SPARQL_RESULT_CSV_VALUE);

    public static final String APPLICATION_SPARQL_RESULT_TSV_VALUE = "application/sparql-results+tsv'";
    public static final SparqlMediaType APPLICATION_SPARQL_RESULT_TSV = fromString(APPLICATION_SPARQL_RESULT_TSV_VALUE);

    public static final String APPLICATION_SPARQL_RESULT_THRIFT_VALUE = "application/sparql-results+thrift'";
    public static final SparqlMediaType APPLICATION_SPARQL_RESULT_THRIFT = fromString(APPLICATION_SPARQL_RESULT_THRIFT_VALUE);

    public static final String TEXT_CSV_VALUE = "text/csv";
    public static final SparqlMediaType TEXT_CSV = fromString(TEXT_CSV_VALUE);

    public static final String TEXT_TSV_VALUE = "text/tab-separated-values";
    public static final SparqlMediaType TEXT_TSV = fromString(TEXT_TSV_VALUE);

    public static final String APPLICATION_SPARQL_RESULT_SSE_VALUE = "application/sparql-results+sse'";
    public static final SparqlMediaType APPLICATION_SPARQL_RESULT_SSE = fromString(APPLICATION_SPARQL_RESULT_SSE_VALUE);

    // TODO: add binary format: thrift

    protected SparqlMediaType(String type) {
        super(type);
    }

    protected SparqlMediaType(String type, String subType) {
        super(type, subType);
    }

}
