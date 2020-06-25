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

package net.sumaris.rdf.dao.referential.taxon;

import net.sumaris.core.dao.technical.Page;
import net.sumaris.rdf.dao.DatabaseResource;
import net.sumaris.rdf.dao.NamedModelProducer;
import net.sumaris.rdf.service.ServiceTestConfiguration;
import net.sumaris.rdf.util.ModelUtils;
import net.sumaris.server.http.rest.RdfFormat;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ServiceTestConfiguration.class})
@TestPropertySource(locations="classpath:sumaris-core-rdf-test.properties")
public class SandreRdfTaxonDaoTest {

    private static final Logger log = LoggerFactory.getLogger(SandreRdfTaxonDaoTest.class);

    @Resource(name = "sandreRdfTaxonDao")
    protected NamedModelProducer sandreRdfTaxonDao;

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Test
    public void loadAllToFile() throws IOException {

        File directory = dbResource.getResourceDirectory("rdf");
        FileUtils.forceMkdir(directory);
        File ttlFile = new File(directory, "taxon-sandre.ttl");

        ttlFile = new File("/home/blavenie/git/sumaris/sumaris-pod/sumaris-core-rdf/src/test/resources", "taxon-sandre.ttl");


        Model model = sandreRdfTaxonDao.loadOnePage(Page.builder().size(1000).build());
        ModelUtils.modelToFile(ttlFile, model, RdfFormat.TURTLE);

        Assume.assumeTrue(false);// Fore failed
    }

    @Test
    public void loadAllByPage() {

        Model model = sandreRdfTaxonDao.loadOnePage(Page.builder().size(100).build());
        byte[] contentBytes = ModelUtils.modelToBytes(model, RdfFormat.TURTLE);

        String content = new String(contentBytes);
        log.debug(content);

        Assert.assertTrue(content.contains("<http://id.eaufrance.fr/apt/"));
        Assert.assertTrue(content.contains("ns4:scientificName  \""));

    }

    /* -- protected functions -- */
}
