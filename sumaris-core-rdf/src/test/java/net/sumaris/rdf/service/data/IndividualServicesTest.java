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

package net.sumaris.rdf.service.data;

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.model.referential.taxon.TaxonName;
import net.sumaris.rdf.AbstractTest;
import net.sumaris.rdf.DatabaseResource;
import net.sumaris.rdf.model.ModelVocabulary;
import net.sumaris.rdf.model.reasoner.ReasoningLevel;
import net.sumaris.rdf.service.schema.RdfSchemaFetchOptions;
import net.sumaris.rdf.service.schema.RdfSchemaService;
import net.sumaris.rdf.util.ModelUtils;
import net.sumaris.rdf.util.RdfFormat;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class IndividualServicesTest extends AbstractTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Resource
    private RdfSchemaService schemaService;

    @Resource
    private RdfIndividualService service;

    private File schemaModelFile;
    private File dataModelFile;


    final String SELECT_ALL_QUERY = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX this: <%s/schema/>\n" +
            "SELECT * WHERE {\n" +
            "  ?sub ?pred ?obj .\n" +
            "} LIMIT 10";

    final String SELECT_BY_LABEL_QUERY = "PREFIX rdf: <" + RDF.getURI() + ">\n" +
            "PREFIX rdfs: <"+ RDFS.getURI() +">\n" +
            "PREFIX this: <%sschema/>\n" +
            "SELECT DISTINCT ?sub\n" +
            "WHERE {\n" +
            "  ?sub rdf:type this:"+ TaxonName.class.getSimpleName() +" ;\n" +
            "    rdfs:label ?label .\n" +
            "  FILTER( regex( ?label, \"^Lophius.*\" ) )\n" +
            "} LIMIT 10";

    @Before
    public void setup() throws IOException {
        this.schemaModelFile = createSchemaModelFile(false);
        this.dataModelFile = createDataModelFile(false);
    }

    @Test
    public void executeQuery() throws IOException {
        Model schema = schemaService.getOntology(RdfSchemaFetchOptions.builder()
                .domain(ModelVocabulary.REFERENTIAL)
                .withEquivalences(false)
                .build());

        Model instances = service.getIndividuals(RdfIndividualFetchOptions.builder()
                .className("TaxonName")
                .id("1001")
                .build());

        Dataset dataset = DatasetFactory.create(schema)
            .setDefaultModel(instances) ;

        String queryString = String.format(SELECT_ALL_QUERY, config.getModelBaseUri());

        try (QueryExecution qExec = QueryExecutionFactory.create(queryString, dataset)) {
            ResultSet rs = qExec.execSelect() ;
            Assert.assertTrue(rs.hasNext());

            printResult(rs);
        }
    }

    @Test
    public void executeUsingConnectionQuery() throws IOException {
        Model instances = service.getIndividuals(RdfIndividualFetchOptions.builder()
                .domain(ModelVocabulary.REFERENTIAL)
                .className("TaxonName")
                .build());

        Model schema = schemaService.getOntology(RdfSchemaFetchOptions.builder()
                .domain(ModelVocabulary.REFERENTIAL)
                .className("TaxonName")
                .withEquivalences(true)
                .build());

        Dataset dataset = DatasetFactory.create() ;
        dataset.setDefaultModel(instances);

        String queryString = String.format(SELECT_BY_LABEL_QUERY, config.getModelBaseUri());


        try (RDFConnection conn = RDFConnectionFactory.connect(dataset)) {
            conn.load(dataModelFile.getPath());
            conn.load(schema);

            QueryExecution qExec = conn.query(queryString);

            ResultSet rs = qExec.execSelect();
            Assert.assertTrue(rs.hasNext());
            printResult(rs);
        }
    }

    /* -- protected functions -- */

    protected File createSchemaModelFile(boolean forceIfExists)  throws IOException {
        Model model = schemaService.getOntology(RdfSchemaFetchOptions.builder()
                .domain(ModelVocabulary.REFERENTIAL)
                .reasoningLevel(ReasoningLevel.NONE)
                .className("TaxonName")
                .build());
        return createModelFile("schema", model, forceIfExists);
    }

    protected File createDataModelFile(boolean forceIfExists)  throws IOException {
        Model model = service.getIndividuals(RdfIndividualFetchOptions.builder()
                .domain(ModelVocabulary.REFERENTIAL)
                .reasoningLevel(ReasoningLevel.NONE)
                .className("TaxonName")
                .build());
        return createModelFile("data", model, forceIfExists);
    }

    protected File createModelFile(String basename, Model model, boolean forceIfExists)  throws IOException {

        File testFile = new File(config.getRdfDirectory(), basename + ".ttl");

        if (testFile.exists() && forceIfExists) {
            testFile.delete();
        }

        if (!testFile.exists()) {
            log.debug("Creating file: " + testFile.getPath());
            outputToFile(model, testFile.toPath(), RdfFormat.TURTLE);
        }
        return testFile;
    }

    protected void printResult(ResultSet rs) {
        while(rs.hasNext()) {
            QuerySolution qs = rs.next();
            log.debug(qs.toString());
        }

    }

    protected void outputToFile(Model model, Path path, RdfFormat format) throws IOException {
        String content = ModelUtils.toString(model, format);
        Files.write(path, content.getBytes());
    }

}
