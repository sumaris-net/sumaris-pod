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

package net.sumaris.rdf.core.service.schema;

import net.sumaris.core.model.ModelVocabularies;
import net.sumaris.core.model.referential.Status;
import net.sumaris.rdf.AbstractTest;
import net.sumaris.rdf.DatabaseResource;
import net.sumaris.rdf.core.model.reasoner.ReasoningLevel;
import net.sumaris.rdf.core.util.ModelUtils;
import net.sumaris.rdf.core.util.RdfFormat;
import org.apache.jena.atlas.lib.StrUtils;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;


public class SchemaServiceTest extends AbstractTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    protected RdfSchemaService schemaService;


    @Test
    public void getOntologyWithRdfsReasoner() {

        // load some data that uses RDFS
        Model individuals = FileManager.get().loadModel("file:src/test/resources/rdf-test-data.ttl");

        // Get schema ontology
        Model model = schemaService.getOntology(RdfSchemaFetchOptions.builder()
                .vocabulary(ModelVocabularies.COMMON)
                .className(Status.class.getSimpleName())
                // Will add RDFS equivalence between:
                // - Status#name <--> rdfs:label
                .withEquivalences(true)
                .reasoningLevel(ReasoningLevel.RDFS)
                .build())
                // Add individuals
                .add(individuals);

        // Get by rdfs:label
        assertQueryHasResult(model, StrUtils.strjoinNL(
                "SELECT * ",
                "WHERE { ",
                "  ?x <"+ RDFS.label.getURI() +"> ?label .",
                "  FILTER ( ?label=\"Actif\" )",
                "}"));
    }

    @Test
    public void getOntologyWithOwlReasoner() {

        // load some data that uses RDFS
        Model individuals = FileManager.get().loadModel("file:src/test/resources/rdf-test-data.ttl");

        // Get schema ontology
        Model model = schemaService.getOntology(RdfSchemaFetchOptions.builder()
                .vocabulary(ModelVocabularies.COMMON)
                .className(Status.class.getSimpleName())
                // Will add OWL equivalence between:
                // - Status#name <--> rdfs:label
                .withEquivalences(true)
                .reasoningLevel(ReasoningLevel.OWL)
                .build())
                // Add individuals
                .add(individuals);

        ModelUtils.toString(model, RdfFormat.TURTLE);

        // Get by rdfs:label
        assertQueryHasResult(model, StrUtils.strjoinNL(
                "SELECT * ",
                "WHERE { ",
                "  ?x <"+ RDFS.label.getURI() +"> ?label .",
                "  FILTER ( ?label=\"Actif\" )",
                "}"));

    }

    /* -- protected functions -- */

    protected void assertQueryHasResult(Model model, String queryString) {
        Query query = QueryFactory.create(queryString);
        try (QueryExecution qExec = QueryExecutionFactory.create(query, model)) {
            ResultSet resultSet = qExec.execSelect();
            Assert.assertTrue(resultSet.hasNext());
        };

    }
}
