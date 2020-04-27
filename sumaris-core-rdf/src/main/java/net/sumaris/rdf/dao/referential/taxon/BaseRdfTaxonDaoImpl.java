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


import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.rdf.dao.NamedModelProducer;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

public abstract class BaseRdfTaxonDaoImpl implements NamedModelProducer {

    private static final Logger log = LoggerFactory.getLogger(BaseRdfTaxonDaoImpl.class);

    @Override
    public Stream<Model> streamAllByPages(long maxStatements) {
        return Daos.streamByPageIteration(this::loadOnePage, m -> !m.isEmpty(), getFetchSize(), maxStatements);
    }

    @Override
    public Model loadOnePage(Page page) {

        String queryString = getConstructQuery(page);
        log.debug("Executing SparQL query on endpoint {{}}...\n{}", getEndpointUrl(), queryString);

        Query query = QueryFactory.create(queryString);
        try (RDFConnection conn = connect(); QueryExecution qexec = conn.query(query)) {

            //QueryExecUtils.executeQuery(qexec);
            return qexec.execConstruct();
        }
    }

    /* -- protected methods  -- */

    protected abstract String getEndpointUrl();

    protected abstract int getFetchSize();

    protected RDFConnection connect() {
        return RDFConnectionFactory.connect(getEndpointUrl());
    }

    protected String getConstructQuery(Page page) {
        return RdfTaxonQueries.getConstructQuery(page);
    }


}
