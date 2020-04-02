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

package net.sumaris.rdf.service.schema;

import net.sumaris.rdf.config.RdfConfiguration;
import net.sumaris.rdf.dao.DatabaseResource;
import net.sumaris.rdf.model.ModelVocabulary;
import net.sumaris.rdf.model.reasoner.ReasoningLevel;
import net.sumaris.rdf.service.AbstractServiceTest;
import net.sumaris.rdf.service.ServiceTestConfiguration;
import org.apache.jena.atlas.lib.StrUtils;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.util.FileManager;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ServiceTestConfiguration.class})
@TestPropertySource(locations="classpath:sumaris-core-rdf-test.properties")
public class RdfSchemaServiceTest extends AbstractServiceTest {


    @Autowired
    protected RdfConfiguration config;

    @Autowired
    protected RdfSchemaService schemaService;

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Test
    public void getOntologyWithRdfsReasoner() {

        // load some data that uses RDFS
        Model data = FileManager.get().loadModel("file:src/test/resources/rdf-test-data.ttl");

        Model model = schemaService.getOntology(RdfSchemaOptions.builder()
                .domain(ModelVocabulary.REFERENTIAL)
                .withEquivalences(true)
                .reasoningLevel(ReasoningLevel.RDFS)
                .build())
                .add(data);

        // Get by rdfs:label
        assertQueryHasResult(model, StrUtils.strjoinNL(
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>",
                "SELECT * ",
                "WHERE { ",
                "  ?x rdfs:label ?label .",
                "  FILTER ( ?label=\"Lophius budegassa\" )",
                "}"));

        // Get by dwc:scientificName
        assertQueryHasResult(model,  StrUtils.strjoinNL(
                "PREFIX dwc: <http://rs.tdwg.org/dwc/terms/>",
                "SELECT * ",
                "WHERE { ",
                "  ?x dwc:scientificName ?label .",
                "  FILTER ( ?label=\"Lophius budegassa\" )",
                "}"));
    }


    /* -- protected functions -- */

    protected void assertQueryHasResult(Model model, String queryString) {
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);

        ResultSet resultSet = qexec.execSelect();
        Assert.assertTrue(resultSet.hasNext());
        //QueryExecUtils.executeQuery(qexec);
    }
}
