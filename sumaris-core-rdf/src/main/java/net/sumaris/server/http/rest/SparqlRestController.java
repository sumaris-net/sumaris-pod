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

package net.sumaris.server.http.rest;

import com.google.common.base.Splitter;
import net.sumaris.rdf.config.RdfConfiguration;
import net.sumaris.rdf.model.ModelDomain;
import net.sumaris.rdf.service.RdfExportOptions;
import net.sumaris.rdf.service.RdfExportService;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.util.List;


@RestController
@ConditionalOnBean({RdfConfiguration.class})
public class SparqlRestController {

    private static final Logger log = LoggerFactory.getLogger(SparqlRestController.class);

    @Resource
    private RdfExportService service;

    @Value( "${rdf.model.uri}" )
    private String rdfModelUri;

    @RequestMapping(
            method = {RequestMethod.GET, RequestMethod.POST},
            value = {
                    RdfRestPaths.SPARQL_PATH
            },
            produces = {
                    SparqlMediaType.APPLICATION_XML_VALUE,
                    SparqlMediaType.APPLICATION_SPARQL_RESULT_XML_VALUE,
                    SparqlMediaType.APPLICATION_JSON_VALUE,
                    SparqlMediaType.APPLICATION_SPARQL_RESULT_JSON_VALUE,
                    SparqlMediaType.TEXT_CSV_VALUE,
                    SparqlMediaType.APPLICATION_SPARQL_RESULT_CSV_VALUE,
                    SparqlMediaType.TEXT_TSV_VALUE,
                    SparqlMediaType.APPLICATION_SPARQL_RESULT_TSV_VALUE,
                    SparqlMediaType.APPLICATION_SPARQL_RESULT_SSE_VALUE,
                    SparqlMediaType.TEXT_HTML_VALUE,
                    SparqlMediaType.TEXT_PLAIN_VALUE
            })
    public ResponseEntity<byte[]> executeRequest(@RequestParam(name = "query") String queryString,
                                                 @RequestHeader(name = HttpHeaders.ACCEPT) String acceptHeader) {

        Query query = QueryFactory.create(queryString) ;

        if (query.getLimit() > 1000) {
            query.setLimit(1000);
        }
        log.info(String.format("Received SparQL query {limit: %s, accept: %s}: \n%s", query.getLimit(), acceptHeader, queryString));


        List<String> acceptedContentTypes = Splitter.on(",").trimResults().splitToList(acceptHeader);

        RdfExportOptions options = RdfExportOptions.builder().withSchema(true)
                .domain(ModelDomain.REFERENTIAL)
                .className("Taxon")
                .build();
        Model model = service.getDataModel(options);

        Dataset dataset = DatasetFactory.create() ;
        dataset.setDefaultModel(model) ;

        try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
            ResultSet rs = qexec.execSelect() ;

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            MediaType contentType = null;
            for (String acceptedContentType: acceptedContentTypes) {
                switch (acceptedContentType) {

                    // XML
                    case SparqlMediaType.APPLICATION_SPARQL_RESULT_XML_VALUE:
                        ResultSetFormatter.outputAsXML(os, rs);
                        contentType = SparqlMediaType.APPLICATION_SPARQL_RESULT_XML;
                        break;

                    // Json
                    case SparqlMediaType.APPLICATION_SPARQL_RESULT_JSON_VALUE:
                        ResultSetFormatter.outputAsJSON(os, rs);
                        contentType = SparqlMediaType.APPLICATION_SPARQL_RESULT_JSON;
                        break;


                    // CSV
                    case SparqlMediaType.TEXT_CSV_VALUE:
                    case SparqlMediaType.APPLICATION_SPARQL_RESULT_CSV_VALUE:
                        ResultSetFormatter.outputAsCSV(os, rs);
                        contentType = SparqlMediaType.APPLICATION_SPARQL_RESULT_CSV;
                        break;

                    // TSV
                    case SparqlMediaType.TEXT_TSV_VALUE:
                    case SparqlMediaType.APPLICATION_SPARQL_RESULT_TSV_VALUE:
                        ResultSetFormatter.outputAsTSV(os, rs);
                        contentType = SparqlMediaType.APPLICATION_SPARQL_RESULT_TSV;
                        break;

                    // SSE
                    case SparqlMediaType.APPLICATION_SPARQL_RESULT_SSE_VALUE:
                        ResultSetFormatter.outputAsSSE(os, rs);
                        contentType = SparqlMediaType.APPLICATION_SPARQL_RESULT_SSE;
                        break;

                    // Other (HTML, text)
                    case SparqlMediaType.TEXT_PLAIN_VALUE:
                    case SparqlMediaType.TEXT_HTML_VALUE:
                        ResultSetFormatter.outputAsTSV(os, rs);
                        contentType = SparqlMediaType.TEXT_PLAIN;
                        break;
                }
                if (contentType != null) {
                    return ResponseEntity.ok()
                            .contentType(contentType)
                            .body(os.toByteArray());
                }
            }
        }

        byte[] errorMessage = String.format("Invalid header {Accept: %s}. Unknown content type.", acceptedContentTypes).getBytes();
        return ResponseEntity.badRequest().body(errorMessage);
    }

    @PostConstruct
    public void afterPropertySet() {
        log.info(String.format("Starting SparQL rest controller at {%s}...", RdfRestPaths.SPARQL_PATH));
    }

}