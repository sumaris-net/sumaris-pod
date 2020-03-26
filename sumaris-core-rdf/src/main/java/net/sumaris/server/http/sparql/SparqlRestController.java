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
import net.sumaris.rdf.service.schema.RdfSchemaService;
import net.sumaris.rdf.util.ModelUtils;
import net.sumaris.server.http.rest.RdfFormat;
import net.sumaris.server.http.rest.RdfMediaType;
import net.sumaris.server.http.rest.RdfRestController;
import org.apache.jena.atlas.lib.StrUtils;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.fuseki.server.Operation;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.dboe.base.file.Location;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.system.Txn;
import org.apache.jena.tdb2.TDB2Factory;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
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
import java.io.File;
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
    private RdfRestController rdfRestController;

    @Resource
    private RdfSchemaService schemaExportService;

    @Resource
    private RdfDataExportService dataExportService;

    @Resource
    private RdfConfiguration config;

    @Value("${server.url}/" + SPARQL_ENDPOINT)
    private String sparqlEndpointUrl;

    @Value("${rdf.tdb2.enabled:false}")
    private boolean enableTdb2;

    private Model defaultModel;
    private Dataset dataset;


    @PostConstruct
    public void init() {
        log.info("Starting SparQL endpoint {{}}...", sparqlEndpointUrl);

        // TODO add inference ?
        //  this.defaultModel = ModelFactory.createInfModel(ReasonerRegistry.getOWLReasoner(), getFullSchemaOntology()).getDeductionsModel();
        this.defaultModel = getFullSchemaOntology();

        // Init the query dataset
        this.dataset = createDataset();

        // TODO: fill dataset ?
        //fillDataset(this.dataset);

//        FusekiServer server = FusekiServer.create()
//                .port(8888)
//                //.loopback(false)
//                .add("/rdf", dataset)
//                .addOperation("/rdf", Operation.Query)
//                .build() ;
//        server.start() ;
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
                                                 @RequestHeader(name = HttpHeaders.ACCEPT) String acceptHeader) {

        Query query = QueryFactory.create(queryString) ;

        if (query.getLimit() > 1000) {
            query.setLimit(1000);
        }
        log.info(String.format("Received SparQL query {limit: %s, accept: %s}: \n%s", query.getLimit(), acceptHeader, queryString));

        List<String> acceptedContentTypes = Splitter.on(",").trimResults().splitToList(acceptHeader);

        Dataset dataset = DatasetFactory.create();
        //dataset.setDefaultModel(rdfRestController.loadModelByUri("http://www.w3.org/2002/07/owl", RdfFormat.RDF));
        fillDataset(dataset);

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

    protected OntModel getFullSchemaOntology() {
        return (OntModel)getReferentialSchemaOntology().add(getDataSchemaOntology());
    }

    protected OntModel getReferentialSchemaOntology() {
        return schemaExportService.getOntology(ModelVocabulary.REFERENTIAL);
    }

    protected OntModel getDataSchemaOntology() {
        return schemaExportService.getOntology(ModelVocabulary.DATA);
    }

    protected Dataset createDataset() {
        if (enableTdb2) {

            // Connect or create the TDB2 dataset
            File tdbDir = new File(config.getRdfDirectory(), "tdb");
            log.info("Starting {TDB2} triple store at {{}}...", tdbDir);

            Location location = Location.create(tdbDir.getAbsolutePath());
            dataset = TDB2Factory.connectDataset(location);
        }
        else {
            log.info("Starting {memory} triple store...");
            dataset = DatasetFactory.create()
                    .setDefaultModel(this.defaultModel);
        }



        return dataset;
    }

    /**
     * Fill dataset
     * @param dataset
     * @return
     */
    protected void fillDataset(Dataset dataset) {

//        dataset.begin(ReadWrite.WRITE);
//        String sparqlUpdateString = StrUtils.strjoinNL(
//                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>",
//                "PREFIX this: <http://192.168.0.20:8080/ontology/schema/>",
//                "INSERT { <http://192.168.0.20:8080/ontology/data/TaxonName/1> rdfs:label ?now } WHERE { BIND(now() AS ?now ) }"
//        ) ;
//
//        UpdateRequest updateRequest = UpdateFactory.create(sparqlUpdateString);
//        UpdateProcessor updateProcessor =
//                UpdateExecutionFactory.create(updateRequest, dataset);
//        updateProcessor.execute();
//        dataset.commit();

        // TODO: change this
        // Load TaxonName
        String graphName = config.getModelBaseUri() + "data/TaxonName";
        Model instances = dataExportService.getIndividuals(RdfDataExportOptions.builder()
                .className("TaxonName")
                .build());

        // TODO enable inferences
        //instances = ModelFactory.createInfModel(ReasonerRegistry.getOWLReasoner(), instances);

        dataset.addNamedModel(graphName, instances);

    }
}