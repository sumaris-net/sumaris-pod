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

package net.sumaris.server.http.sparql;

import com.google.common.base.Splitter;
import net.sumaris.core.util.StringUtils;
import net.sumaris.rdf.config.RdfConfiguration;
import net.sumaris.rdf.model.ModelVocabulary;
import net.sumaris.rdf.service.data.RdfDataExportOptions;
import net.sumaris.rdf.service.data.RdfDataExportService;
import net.sumaris.rdf.service.schema.RdfSchemaExportOptions;
import net.sumaris.rdf.service.schema.RdfSchemaExportService;
import net.sumaris.rdf.util.ModelUtils;
import net.sumaris.server.http.rest.RdfFormat;
import net.sumaris.server.http.rest.RdfMediaType;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


@RestController
@ConditionalOnBean({RdfConfiguration.class})
public class SparqlRestController {

    public static final String SPARQL_ENDPOINT = "/sparql";

    private static final Logger log = LoggerFactory.getLogger(SparqlRestController.class);

    @Resource
    private RdfSchemaExportService schemaExportService;

    @Resource
    private RdfDataExportService dataExportService;

    @Resource
    private RdfConfiguration config;

    @Value("${server.url}/" + SPARQL_ENDPOINT)
    private String sparqlEndpointUrl;

    @PostConstruct
    public void init() {
        log.info("Starting SparQL endpoint {{}}...", sparqlEndpointUrl);
    }

    @RequestMapping(
            method = {RequestMethod.GET, RequestMethod.POST},
            value = {
                    SPARQL_ENDPOINT
            },
            produces = {
                    // Ask, Select
                    SparqlMediaType.APPLICATION_XML_VALUE,
                    SparqlMediaType.APPLICATION_SPARQL_RESULT_XML_VALUE,
                    SparqlMediaType.APPLICATION_JSON_VALUE,
                    SparqlMediaType.APPLICATION_SPARQL_RESULT_JSON_VALUE,
                    SparqlMediaType.TEXT_CSV_VALUE,
                    SparqlMediaType.APPLICATION_SPARQL_RESULT_CSV_VALUE,
                    SparqlMediaType.TEXT_TSV_VALUE,
                    SparqlMediaType.APPLICATION_SPARQL_RESULT_TSV_VALUE,
                    SparqlMediaType.APPLICATION_SPARQL_RESULT_SSE_VALUE,
                    SparqlMediaType.APPLICATION_SPARQL_RESULT_THRIFT_VALUE,
                    // Construct, Describe
                    RdfMediaType.APPLICATION_RDF_XML_VALUE,
                    RdfMediaType.APPLICATION_RDF_JSON_VALUE,
                    RdfMediaType.APPLICATION_JSON_LD_VALUE,
                    RdfMediaType.TEXT_N3_VALUE,
                    RdfMediaType.APPLICATION_N_TRIPLES_VALUE,
                    RdfMediaType.APPLICATION_N_QUADS_VALUE,
                    RdfMediaType.APPLICATION_TRIG_VALUE,
                    RdfMediaType.TEXT_TRIG_VALUE,
                    RdfMediaType.APPLICATION_TRIX_VALUE,
                    RdfMediaType.TEXT_TRIX_VALUE,
                    RdfMediaType.APPLICATION_TURTLE_VALUE,
                    RdfMediaType.TEXT_TURTLE_VALUE,
                    RdfMediaType.TEXT_TURTLE_VALUE,
                    // Text and Html (for browsers)
                    MediaType.TEXT_HTML_VALUE,
                    MediaType.TEXT_PLAIN_VALUE

            })
    public ResponseEntity<byte[]> executeRequest(@RequestParam(name = "query") String queryString,
                                                 @RequestParam(name = "service", required = false) String externalService,
                                                 @RequestHeader(name = HttpHeaders.ACCEPT) String acceptHeader) {

        Query query = QueryFactory.create(queryString) ;

        if (query.getLimit() > 1000) {
            query.setLimit(1000);
        }
        log.info(String.format("Received SparQL query {limit: %s, accept: %s}: \n%s", query.getLimit(), acceptHeader, queryString));

        List<String> acceptedContentTypes = Splitter.on(",").trimResults().splitToList(acceptHeader);

        // Execute into an external endpoint
        if (StringUtils.isNotBlank(externalService) && !externalService.startsWith(sparqlEndpointUrl)) {
            // TODO
        }

        // Load the schema
        Model schema = schemaExportService.getSchemaOntology( RdfSchemaExportOptions.builder()
                .domain(ModelVocabulary.REFERENTIAL)
                .className("TaxonName")
                .build());

        // Load instances (as individuals)
        Model instances = dataExportService.getIndividuals(RdfDataExportOptions.builder()
                .domain(ModelVocabulary.REFERENTIAL) // TODO change this
                .className("TaxonName") // TODO change this
                .build());

        Dataset dataset = DatasetFactory.create(instances);
        dataset.setDefaultModel(schema);

        //        try (QueryExecution qExec = QueryExecutionFactory.create(query, dataset)) {
        Optional<ResponseEntity<byte[]>> response;
        try (RDFConnection conn = RDFConnectionFactory.connect(dataset); QueryExecution qExec = conn.query(query)) {

            // Construct / Describe query
            if (query.isConstructType()) {
                Model model = qExec.execConstruct();
                response = outputModel(model, acceptedContentTypes);
            }
            else if (query.isDescribeType()) {
                Model model = qExec.execDescribe();
                response = outputModel(model, acceptedContentTypes);
            }

            // Ask / Select query
            else if (query.isAskType()) {
                response = outputBoolean(qExec.execAsk(), acceptedContentTypes);
            }
            else if (query.isSelectType()) {
                response = outputResultSet(qExec.execSelect(), acceptedContentTypes);
            }

            // Unknown, or not supported type
            else if (query.isUnknownType()) {
                return ResponseEntity.badRequest()
                        .body("Unknown query type".getBytes());
            }
            else {
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                        .body(String.format("SparQL query of type %s is not supported yet by this endpoint.", query.getQueryType()).getBytes());
            }
        }

        return response
                .orElseGet(() -> ResponseEntity.badRequest()
                .body(String.format("Invalid header {Accept: %s}. Unknown content type.", acceptedContentTypes).getBytes()));

    }

    protected Optional<ResponseEntity<byte[]>> outputResultSet(
            final ResultSet rs,
            final Collection<String> acceptedContentTypes) {


        return firstValidFormat(acceptedContentTypes, SparqlResultSetFormat::fromContentType)
                .map(format -> {
                    // Convert result set to bytes
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    ResultSetFormatter.output(os, rs, format.toResultsFormat());

                    // Return response
                    return ResponseEntity.ok()
                            .contentType(format.mineType())
                            .body(os.toByteArray());
                });
    }


    protected Optional<ResponseEntity<byte[]>> outputModel(
            final Model model,
            final Collection<String> acceptedContentTypes) {

        return firstValidFormat(acceptedContentTypes, RdfFormat::fromContentType)
                .map(format -> {
                    // Convert model to bytes
                    byte[] content = ModelUtils.modelToBytes(model, format);
                    // Return response
                    return ResponseEntity.ok()
                            .contentType(format.mineType())
                            .body(content);
                });
    }

    protected Optional<ResponseEntity<byte[]>> outputBoolean(
            final boolean result,
            final Collection<String> acceptedContentTypes) {


        return firstValidFormat(acceptedContentTypes, RdfFormat::fromContentType)
                .map(format -> {
                    // Convert boolean to bytes
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    ResultSetFormatter.output(os, result, format);

                    // Return response
                    return ResponseEntity.ok()
                            .contentType(format.mineType())
                            .body(os.toByteArray());
                });
    }

    protected <U> Optional<U> firstValidFormat(Collection<String> acceptedContentTypes, Converter<String, Optional<U>> converter) {
        return acceptedContentTypes.stream()
                .map(acceptedContentType -> converter.convert(acceptedContentType).orElse(null))
                .filter(Objects::nonNull)
                .findFirst();
    }

}