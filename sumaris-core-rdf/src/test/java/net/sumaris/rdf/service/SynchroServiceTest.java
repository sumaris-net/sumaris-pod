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

import net.sumaris.rdf.dao.DatabaseResource;
import org.apache.jena.ontology.OntModel;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class SynchroServiceTest extends AbstractServiceTest{

    private static final Logger log = LoggerFactory.getLogger(SynchroServiceTest.class);

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private RdfImportServiceImpl service;

    @Test
    public void getRemoteModel() {
        String ontologyUrl = dbResource.getFixtures().getRemoteOntologyUrl();
        OntModel remoteModel = service.getRemoteModel(ontologyUrl);
        Assert.assertNotNull(remoteModel);
    }

    @Test
    @Ignore
    public void testSynchro() {
        String ontologyUrl = dbResource.getFixtures().getRemoteOntologyUrl();
        String ontologyIri = dbResource.getFixtures().getRemoteOntologyIri();
        OntModel mappedModel = service.importFromRemote(ontologyUrl, ontologyIri, "referential", "net.sumaris.core.model.referential");
        Assert.assertNotNull(mappedModel);
    }
}
