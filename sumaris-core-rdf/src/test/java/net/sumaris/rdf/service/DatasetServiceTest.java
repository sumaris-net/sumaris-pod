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

import graphql.Assert;
import net.sumaris.rdf.AbstractTest;
import net.sumaris.rdf.DatabaseResource;
import net.sumaris.rdf.service.store.DatasetService;
import org.apache.jena.query.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.system.Txn;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;
import java.util.stream.Collectors;

public class DatasetServiceTest extends AbstractTest {

    private static final Logger log = LoggerFactory.getLogger(DatasetServiceTest.class);

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Autowired
    private DatasetService service;

    @Test
    public void getModelNames() {
        Set<String> names = service.getModelNames();
        Assert.assertTrue(names.size() > 0);
        log.info(names.stream().collect(Collectors.joining(", ")));
    }

    @Test
    public void testDataLoaded() {



        // Connect or create the TDB2 dataset
        Dataset ds = service.getDataset();

        //Txn.executeWrite(ds, () -> RDFDataMgr.read(ds, "file:src/test/resources/rdf-test-data.ttl")) ;
        Txn.executeRead(ds, () -> RDFDataMgr.write(System.out, ds, Lang.TRIG));
    }

}
