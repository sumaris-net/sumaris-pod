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

package net.sumaris.rdf.core.util;

import de.uni_stuttgart.vis.vowl.owl2vowl.Owl2Vowl;
import graphql.Assert;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.util.StringUtils;
import net.sumaris.rdf.AbstractTest;
import net.sumaris.rdf.DatabaseResource;
import net.sumaris.rdf.core.model.ModelURIs;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.FileManager;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;

import java.io.File;

//@Ignore
// FIXME BLA failed
@Slf4j
public class ModelUtilsTest extends AbstractTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Test
    public void convertFoafToVowl() {

        String modelUri = ModelURIs.RDF_URL_BY_PREFIX.get("foaf");
        Model model = ModelUtils.read(modelUri, RdfFormat.RDFXML);

        String modelVowl1 = new Owl2Vowl(IRI.create(modelUri)).getJsonAsString();
        log.info(modelVowl1);

        String modelVowl = ModelUtils.toString(model, RdfFormat.VOWL);
        log.info(modelVowl);

    }

    @Test
    public void convertRdfFileToVowl() {
        File tempFile = new File("src/test/resources/rdf-test-data.ttl");
        Model tempModel = FileManager.get().loadModel("file:" + tempFile.getAbsolutePath());

        Model model = ModelFactory.createDefaultModel()
                .add(tempModel);

        String vowl = ModelUtils.toString(model, RdfFormat.RDFXML);
        Assert.assertTrue(StringUtils.isNotBlank(vowl));
    }

    @Test
    public void convertSandreOwlUriToVowl() {

        String modelUri = ModelURIs.RDF_URL_BY_PREFIX.get("apt");
        Assume.assumeTrue("Missing URI for 'apt' prefix!", StringUtils.isNotBlank(modelUri));

        Model model = ModelUtils.read(modelUri, RdfFormat.OWL);
        String modelVowl = ModelUtils.toString(model, RdfFormat.VOWL);
        log.info(modelVowl);
    }


    @Test
    public void convertSandreOwlFileToVowl() {
        File modelFile = new File("src/test/resources/sandre_fmt_owl_apt.owl");
        Assume.assumeTrue("Missing OWL file: " + modelFile.getAbsolutePath(), modelFile.exists());

        Model model = ModelUtils.read(modelFile, null);

        String owl = ModelUtils.toString(model, RdfFormat.OWL);
        log.info(owl);
        //Assert.assertTrue(StringUtils.isNotBlank(owl));

    }

}