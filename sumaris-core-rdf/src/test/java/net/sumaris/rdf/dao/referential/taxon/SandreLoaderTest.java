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
import net.sumaris.core.util.Files;
import net.sumaris.rdf.AbstractTest;
import net.sumaris.rdf.DatabaseResource;
import net.sumaris.rdf.loader.NamedRdfLoader;
import net.sumaris.rdf.util.ModelUtils;
import net.sumaris.rdf.util.RdfFormat;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;

public class SandreLoaderTest extends AbstractTest {

    private static final Logger log = LoggerFactory.getLogger(SandreLoaderTest.class);

    @Resource(name = "sandreTaxonLoader")
    protected NamedRdfLoader loader;

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Test
    public void loadAllToFile() throws IOException {

        File directory = dbResource.getResourceDirectory("rdf");
        FileUtils.forceMkdir(directory);
        File file = new File(directory, "taxon-sandre.ttl");
        Files.deleteQuietly(file);

        Model model = loader.loadOnePage(Page.builder().size(1000).build());

        ModelUtils.write(model, RdfFormat.TURTLE, file);

        Assume.assumeTrue(file.exists());
    }

    @Test
    public void loadAllByPage() {

        Model model = loader.loadOnePage(Page.builder().size(10).build());
        String content = ModelUtils.toString(model, RdfFormat.TURTLE);
        log.debug(content);

        Assert.assertTrue(content.contains("<http://id.eaufrance.fr/apt/"));
        Assert.assertTrue(content.contains(":scientificName "));

    }

    /* -- protected functions -- */
}