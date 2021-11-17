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

package net.sumaris.rdf.core.dao.referential.taxon;

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.rdf.AbstractTest;
import net.sumaris.rdf.DatabaseResource;
import net.sumaris.rdf.core.loader.INamedRdfLoader;
import net.sumaris.rdf.core.util.ModelUtils;
import net.sumaris.rdf.core.util.RdfFormat;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;

@Slf4j
public class TaxrefLoaderTest extends AbstractTest {

    @Resource(name = "mnhnTaxonLoader")
    protected INamedRdfLoader loader;

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Test
    public void loadAllToFile() throws IOException {

        File directory = dbResource.getResourceDirectory("rdf");
        FileUtils.forceMkdir(directory);
        File ttlFile = new File(directory, "taxon-sandre.ttl");

        Model model = loader.loadOnePage(Page.builder().size(1000).build());
        ModelUtils.write(model, RdfFormat.TURTLE, ttlFile);

        Assert.assertTrue(ttlFile.exists());
    }

    @Test
    public void loadOnePage() {

        Model model = loader.loadOnePage(Page.builder().size(100).build());
        String content = ModelUtils.toString(model, RdfFormat.TURTLE);
        //log.debug(content);

        Assert.assertTrue(content.contains("<http://taxref.mnhn.fr/lod/name/"));
        Assert.assertTrue(content.contains("scientificName  \""));

    }

    /* -- protected functions -- */
}
