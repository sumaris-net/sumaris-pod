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

package net.sumaris.server.http.sparql;

import net.sumaris.rdf.TestConfiguration;
import net.sumaris.rdf.config.RdfConfiguration;
import net.sumaris.rdf.service.ServiceTestConfiguration;
import org.apache.jena.atlas.lib.StrUtils;
import org.apache.jena.dboe.base.file.Location;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.system.Txn;
import org.apache.jena.tdb2.TDB2Factory;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ServiceTestConfiguration.class})
@TestPropertySource(locations="classpath:sumaris-core-rdf-test.properties")
public class FusekiTest {

    @Autowired
    protected RdfConfiguration config;

    @Test
    public void insertDummyIntoTB2() {

        // Connect or create the TDB2 dataset
        File tdbDir = new File(config.getRdfDirectory(), "tdb");
        Location location = Location.create(tdbDir.getAbsolutePath());
        Dataset dataset = TDB2Factory.connectDataset(location);

        dataset.begin(ReadWrite.WRITE);
        String sparqlUpdateString = StrUtils.strjoinNL(
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>",
                "PREFIX this: <http://192.168.0.20:8080/ontology/schema/>",
                "INSERT { <this:1> rdfs:label ?now } WHERE { BIND(now() AS ?now ) }"
        ) ;

        UpdateRequest updateRequest = UpdateFactory.create(sparqlUpdateString);
        UpdateProcessor updateProcessor =
                UpdateExecutionFactory.create(updateRequest, dataset);
        updateProcessor.execute();
        dataset.commit();
        dataset.close();
    }

    @Test
    public void insertTB2() {

        // Connect or create the TDB2 dataset
        File tdbDir = new File(config.getRdfDirectory(), "tdb");
        Location location = Location.create(tdbDir.getAbsolutePath());
        Dataset ds = TDB2Factory.connectDataset(location);

        Txn.executeWrite(ds, ()->{
            RDFDataMgr.read(ds, "SomeData.ttl");
            RDFDataMgr.read(ds, "SomeData.ttl");
        }) ;
//        Txn.execRead(dsg, ()->{
//            RDFDataMgr.write(System.out, ds, Lang.TRIG) ;
//        }) ;
    }
}
