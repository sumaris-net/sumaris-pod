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

package net.sumaris.rdf.core.loader.taxon;


import lombok.extern.slf4j.Slf4j;
import net.sumaris.rdf.core.loader.AbstractNamedRdfLoader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("sandreTaxonRdfLoader")
@Slf4j
public class SandreTaxonRdfLoader extends AbstractNamedRdfLoader {

    @Value("${rdf.sandre.sparql.endpoint:http://id.eaufrance.fr/sparql}")
    private String endpointUrl;

    @Value("${rdf.sandre.sparql.limit:10000}")
    private int fetchSize = 10000;

    @Value("${rdf.dataset.sandre.apt.name:http://id.eaufrance.fr/apt/}")
    private String name;

    @Value("${rdf.dataset.sandre.apt.query:classpath:sparql/taxon.sparql}")
    private String queryFile;

    @Override
    public boolean enable() {
        return true;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    protected int getFetchSize() {
        return fetchSize;
    }

    @Override
    protected String getEndpointUrl() {
        return endpointUrl;
    }

    @Override
    protected String getQueryPath() {
        return queryFile;
    }

    /* -- protected functions -- */

    @Override
    protected String getQuery() {
        // TODO : add filter like: FILTER( ?modified >= "2020-05-23T00:00:00"^^xsd:dateTime)
        return super.getQuery();
    }
}
