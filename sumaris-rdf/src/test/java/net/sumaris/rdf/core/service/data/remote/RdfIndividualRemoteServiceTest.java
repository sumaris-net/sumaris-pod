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

package net.sumaris.rdf.core.service.data.remote;

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.model.ModelVocabularies;
import net.sumaris.rdf.AbstractTest;
import net.sumaris.rdf.DatabaseResource;
import net.sumaris.rdf.core.model.ModelVocabulary;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class RdfIndividualRemoteServiceTest extends AbstractTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private RdfIndividualRemoteServiceImpl service;

    @Test
    @Ignore
    public void getRemoteModel() {
        String ontologyUrl = fixtures.getRemoteOntologyUrl();
        OntModel remoteModel = service.getRemoteModel(ontologyUrl);
        Assert.assertNotNull(remoteModel);
    }

    @Test
    //@Ignore
    public void testSynchro() {
        String ontologyUrl = fixtures.getRemoteOntologyUrl();
        String ontologyIri = fixtures.getRemoteOntologyIri();
        String ontologyVocabulary = fixtures.getRemoteOntologyVocabulary();
        Model mappedModel = service.importFromRemote(ontologyUrl, ontologyIri, ontologyVocabulary, "net.sumaris.core.model.referential");
        Assert.assertNotNull(mappedModel);
    }
}
