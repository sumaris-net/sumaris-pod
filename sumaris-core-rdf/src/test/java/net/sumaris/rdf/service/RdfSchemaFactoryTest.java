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

package net.sumaris.rdf.service;

import net.sumaris.rdf.model.ModelVocabulary;
import net.sumaris.rdf.service.data.RdfDataExportOptions;
import net.sumaris.rdf.service.data.RdfDataExportService;
import net.sumaris.rdf.service.schema.RdfSchemaOptions;
import net.sumaris.rdf.service.schema.RdfSchemaService;
import net.sumaris.rdf.util.ModelUtils;
import net.sumaris.server.http.rest.RdfFormat;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RdfSchemaFactoryTest extends AbstractServiceTest {



    private static final Logger log = LoggerFactory.getLogger(RdfSchemaFactoryTest.class);

    @Resource
    private RdfSchemaService schemaExportService;

    @Resource
    private RdfDataExportService dataExportService;

    private File schemaModelFile;
    private File dataModelFile;


    final String QUERY_ALL = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX this: <http://192.168.0.20:8080/ontology/schema/>\n" +
            "SELECT * WHERE {\n" +
            "  ?sub ?pred ?obj .\n" +
            "} LIMIT 10";

    final String QUERY2 = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX this: <http://192.168.0.20:8080/ontology/schema/>\n" +
            "SELECT * WHERE {\n" +
            "  ?sub rdf:type this:TaxonName .\n" +
            "\tfilter( regex( ?label, \"^Lophius.*\" ) ) \n" +
            "} LIMIT 10";

    @Before
    public void setup() throws IOException {
        this.schemaModelFile = createSchemaModelFile(false);
        this.dataModelFile = createDataModelFile(false);
    }

    @Test
    public void executeQuery() throws IOException {
        Model schema = schemaExportService.getOntology(RdfSchemaOptions.builder()
                .domain(ModelVocabulary.REFERENTIAL)
                .withEquivalences(false)
                .build());

        Model instances = dataExportService.getIndividuals(RdfDataExportOptions.builder()
                .className("TaxonName")
                .id("1001")
                .build());

        Dataset dataset = DatasetFactory.create(schema)
            .setDefaultModel(instances) ;


        try (QueryExecution qExec = QueryExecutionFactory.create(QUERY_ALL, dataset)) {
            ResultSet rs = qExec.execSelect() ;
            Assert.assertTrue(rs.hasNext());

            printResult(rs);
        }
    }

    @Test
    public void executeUsingConnectionQuery() throws IOException {
        Model instances = dataExportService.getIndividuals(RdfDataExportOptions.builder()
                .domain(ModelVocabulary.REFERENTIAL)
                .className("TaxonName")
                .build());

        Model schema = schemaExportService.getOntology(RdfSchemaOptions.builder()
                .domain(ModelVocabulary.REFERENTIAL)
                .className("TaxonName")
                .withEquivalences(true)
                .build());

        Dataset dataset = DatasetFactory.create() ;
        dataset.setDefaultModel(instances);

        try (RDFConnection conn = RDFConnectionFactory.connect(dataset)) {
            conn.load(schemaModelFile.getPath());
            conn.load(schema);

            QueryExecution qExec = conn.query(QUERY2);

            ResultSet rs = qExec.execSelect();
            Assert.assertTrue(rs.hasNext());
            printResult(rs);
        }
    }

    /* -- -- */

    protected File createSchemaModelFile(boolean forceIfExists)  throws IOException {
        Model model = schemaExportService.getOntology(RdfSchemaOptions.builder()
                .domain(ModelVocabulary.REFERENTIAL)
                .className("TaxonName")
                .build());
        return createModelFile("schema", model, forceIfExists);
    }

    protected File createDataModelFile(boolean forceIfExists)  throws IOException {
        Model model = dataExportService.getIndividuals(RdfDataExportOptions.builder()
                .domain(ModelVocabulary.REFERENTIAL)
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
            log.info(qs.toString());
        }

    }

    protected void outputToFile(Model model, Path path, RdfFormat format) throws IOException {
        String content = ModelUtils.modelToString(model, format);
        Files.write(path, content.getBytes());
    }

}
