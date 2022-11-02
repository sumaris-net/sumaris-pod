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

package net.sumaris.rdf.server.http.rest.sparql;

import net.sumaris.rdf.core.util.RdfMediaType;
import org.apache.jena.sparql.resultset.ResultsFormat;
import org.springframework.http.MediaType;

import java.util.Optional;

public enum SparqlResultSetFormat {

    RS_XML(ResultsFormat.FMT_RS_XML, SparqlMediaType.APPLICATION_SPARQL_RESULT_XML),
    RS_JSON(ResultsFormat.FMT_RS_JSON, SparqlMediaType.APPLICATION_SPARQL_RESULT_JSON),
    RS_CSV(ResultsFormat.FMT_RS_CSV, SparqlMediaType.APPLICATION_SPARQL_RESULT_CSV),
    RS_SSE(ResultsFormat.FMT_RS_SSE, SparqlMediaType.APPLICATION_SPARQL_RESULT_SSE),
    RS_TSV(ResultsFormat.FMT_RS_TSV, SparqlMediaType.APPLICATION_SPARQL_RESULT_TSV),
    RS_THRIFT(ResultsFormat.FMT_RS_THRIFT, SparqlMediaType.APPLICATION_SPARQL_RESULT_THRIFT)
    ;

    private ResultsFormat jenaFormat;
    private MediaType contentType;

    SparqlResultSetFormat(ResultsFormat jenaFormat, MediaType contentType) {
        this.jenaFormat = jenaFormat;
        this.contentType = contentType;
    }

    public ResultsFormat toResultsFormat() {
        return jenaFormat;
    }

    public MediaType mineType() {
        return contentType;
    }


    public static Optional<SparqlResultSetFormat> fromContentType(String contentType) throws IllegalArgumentException{

        switch (contentType.toLowerCase()) {
            // Xml
            case RdfMediaType.TEXT_XML_VALUE:
            case RdfMediaType.APPLICATION_XML_VALUE:
            case SparqlMediaType.APPLICATION_SPARQL_RESULT_XML_VALUE:
                return Optional.of(RS_XML);

            // Json
            case SparqlMediaType.APPLICATION_JSON_VALUE:
            case SparqlMediaType.APPLICATION_JSON_UTF8_VALUE:
            case SparqlMediaType.APPLICATION_SPARQL_RESULT_JSON_VALUE:
                return Optional.of(RS_JSON);

            // CSV
            case SparqlMediaType.TEXT_CSV_VALUE:
            case SparqlMediaType.APPLICATION_SPARQL_RESULT_CSV_VALUE:
                return Optional.of(RS_CSV);

            // SSE
            case SparqlMediaType.APPLICATION_SPARQL_RESULT_SSE_VALUE:
                return Optional.of(RS_SSE);

            // TSV
            case SparqlMediaType.TEXT_TSV_VALUE:
            case SparqlMediaType.APPLICATION_SPARQL_RESULT_TSV_VALUE:
                return Optional.of(RS_TSV);

            // Other (Text, HTML) => TSV
            case SparqlMediaType.TEXT_PLAIN_VALUE:
            case RdfMediaType.TEXT_HTML_VALUE:
            case RdfMediaType.APPLICATION_XHTML_XML_VALUE:
                return Optional.of(RS_TSV);

        }

        return Optional.empty();

    }
}
