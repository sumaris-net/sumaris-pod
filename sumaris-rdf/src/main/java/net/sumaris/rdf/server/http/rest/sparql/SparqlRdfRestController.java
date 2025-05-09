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

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.util.StringUtils;
import net.sumaris.rdf.core.config.RdfConfiguration;
import net.sumaris.rdf.core.service.store.RdfDatasetService;
import net.sumaris.rdf.core.util.ModelUtils;
import net.sumaris.rdf.core.util.RdfFormat;
import net.sumaris.rdf.core.util.RdfMediaType;
import net.sumaris.rdf.server.http.rest.RdfRestPaths;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.sparql.core.Transactional;
import org.apache.jena.sparql.resultset.ResultsFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
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
@ConditionalOnWebApplication
@Slf4j
public class SparqlRdfRestController implements RdfRestPaths {

    @Value("${rdf.sparql.maxLimit:10000}")
    private long maxLimit;

    @Resource
    private RdfDatasetService rdfDatasetService;

    @PostConstruct
    public void init() {
        log.info("Starting SparQL endpoint {{}}...", SPARQL_ENDPOINT);
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
            SparqlMediaType.APPLICATION_JSON_UTF8_VALUE,
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
    public ResponseEntity<byte[]> executeRequest(@RequestParam(name = "query") java.lang.String queryString,
                                                 @RequestParam(name = "service", required = false) java.lang.String service,
                                                 @RequestHeader(name = HttpHeaders.ACCEPT) java.lang.String acceptHeader) {

        List<java.lang.String> acceptedContentTypes = Splitter.on(",").trimResults().splitToList(acceptHeader);
        Query query = QueryFactory.create(queryString);

        long limit = query.getLimit() < 0 ? -1L : query.getLimit();
        log.info(java.lang.String.format("Received SparQL query {limit: %s, accept: %s}: \n%s", limit, acceptHeader, queryString));

        // Limit to the max
        if (limit > maxLimit) {
            query.setLimit(maxLimit);
            log.warn("Reducing limit to the max {{}}", maxLimit);
        }

        // Remote execution
        if (StringUtils.isNotBlank(service)) {
            try (RDFConnection conn = RDFConnectionFactory.connect(service); QueryExecution qexec = conn.query(query)) {
                return executeQuery(conn, qexec, acceptedContentTypes);
            }
        }

        // Local execution
        else {
            // Construct the dataset for this query
            Dataset dataset = rdfDatasetService.prepareDatasetForQuery(query);

            try (RDFConnection conn = RDFConnectionFactory.connect(dataset); QueryExecution qexec = conn.query(query)) {
                return executeQuery(conn, qexec, acceptedContentTypes);
            }
        }
    }

    protected ResponseEntity<byte[]> executeQuery(Transactional transactional,
                                                  QueryExecution qExec,
                                                  List<java.lang.String> acceptedContentTypes ) {
        Preconditions.checkNotNull(transactional);
        Preconditions.checkNotNull(qExec);
        Preconditions.checkNotNull(acceptedContentTypes);

        Optional<ResponseEntity<byte[]>> response;

        try {
            // Construct Quad query
            Query query = qExec.getQuery();
            if (query.isConstructQuad()) {
                if (!transactional.isInTransaction()) transactional.begin(ReadWrite.READ);
                Dataset resultDataset = qExec.execConstructDataset();
                response = outputDataset(resultDataset,
                    new ImmutableList.Builder<java.lang.String>()
                        .addAll(acceptedContentTypes)
                        .add(ResultsFormat.FMT_RDF_TRIG.getSymbol())
                        .build());
            }

            // Construct query
            else if (query.isConstructType()) {
                if (!transactional.isInTransaction()) transactional.begin(ReadWrite.READ);
                Model model = qExec.execConstruct();
                response = outputModel(model, acceptedContentTypes);
            }

            // Describe query
            else if (query.isDescribeType()) {
                if (!transactional.isInTransaction()) transactional.begin(ReadWrite.READ);
                Model model = qExec.execDescribe();
                response = outputModel(model, acceptedContentTypes);
            }

            // Ask query
            else if (query.isAskType()) {
                if (!transactional.isInTransaction()) transactional.begin(ReadWrite.READ);
                response = outputBoolean(qExec.execAsk(), acceptedContentTypes);
            }

            // Select query
            else if (query.isSelectType()) {
                if (!transactional.isInTransaction()) transactional.begin(ReadWrite.READ);
                response = outputResultSet(qExec.execSelect(), acceptedContentTypes);
            }

            // TODO: add update ?

            // Unknown, or not supported type
            else if (query.isUnknownType()) {
                return ResponseEntity.badRequest()
                    .body("Unknown query type".getBytes());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                    .body(java.lang.String.format("SparQL query of type %s is not supported yet by this endpoint.", query.queryType()).getBytes());
            }

            return response
                .orElseGet(() -> ResponseEntity.badRequest()
                    .body(java.lang.String.format("Invalid header {Accept: %s}. Unknown content type.", acceptedContentTypes).getBytes()));
        }
        finally {
            if (transactional.isInTransaction())  transactional.end();
        }
    }

    protected Optional<ResponseEntity<byte[]>> outputResultSet(
        final ResultSet rs,
        final Collection<java.lang.String> acceptedContentTypes) {


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
        final Collection<java.lang.String> acceptedContentTypes) {

        return firstValidFormat(acceptedContentTypes, RdfFormat::fromContentType)
            .map(format -> {
                // Convert model to bytes
                byte[] content = ModelUtils.toBytes(model, format);
                // Return response
                return ResponseEntity.ok()
                    .contentType(format.mineType())
                    .body(content);
            });
    }

    protected Optional<ResponseEntity<byte[]>> outputDataset(
        final Dataset dataset,
        final Collection<java.lang.String> acceptedContentTypes) {

        return firstValidFormat(acceptedContentTypes, RdfFormat::fromContentType)
            .map(format -> {
                // Convert model to bytes
                byte[] content = ModelUtils.toBytes(dataset, format);
                // Return response
                return ResponseEntity.ok()
                    .contentType(format.mineType())
                    .body(content);
            });
    }

    protected Optional<ResponseEntity<byte[]>> outputBoolean(
        final boolean result,
        final Collection<java.lang.String> acceptedContentTypes) {


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

    protected <U> Optional<U> firstValidFormat(Collection<java.lang.String> acceptedContentTypes, Converter<java.lang.String, Optional<U>> converter) {
        return acceptedContentTypes.stream()
            .map(acceptedContentType -> converter.convert(acceptedContentType).orElse(null))
            .filter(Objects::nonNull)
            .findFirst();
    }

}