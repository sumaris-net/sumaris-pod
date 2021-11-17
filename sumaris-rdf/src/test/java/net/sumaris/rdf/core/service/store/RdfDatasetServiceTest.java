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

package net.sumaris.rdf.core.service.store;

import graphql.Assert;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.rdf.AbstractTest;
import net.sumaris.rdf.DatabaseResource;
import org.apache.jena.query.Dataset;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.system.Txn;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class RdfDatasetServiceTest extends AbstractTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Autowired
    private RdfDatasetService service;

    @Test
    public void getModelNames() {
        Set<String> names = service.getModelNames();
        Assert.assertTrue(names.size() > 0);
        if (log.isDebugEnabled()) log.debug(names.stream().collect(Collectors.joining(", ")));
    }

    @Test
    public void testDataLoaded() throws Exception {
        // Connect or create the TDB2 dataset
        Dataset ds = service.getDataset();

        File tmpFile = new File(dbResource.getResourceDirectory("data"), "testDataLoaded.ttl");
        dbResource.addToDestroy(tmpFile);

        try (FileOutputStream fos = new FileOutputStream(tmpFile)){
            Txn.executeRead(ds, () -> RDFDataMgr.write(fos, ds, Lang.TRIG));
        }


    }

}
