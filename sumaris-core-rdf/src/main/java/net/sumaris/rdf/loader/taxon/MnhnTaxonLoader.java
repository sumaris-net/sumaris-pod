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

package net.sumaris.rdf.loader.taxon;


import net.sumaris.rdf.loader.AbstractNamedRdfLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("mnhnTaxonLoader")
public class MnhnTaxonLoader extends AbstractNamedRdfLoader {

    private static final Logger log = LoggerFactory.getLogger(MnhnTaxonLoader.class);

    @Value("${rdf.taxref.sparql.endpoint:http://taxref.mnhn.fr/sparql}")
    private String endpointUrl;

    @Value("${rdf.taxref.sparql.limit:10000}")
    private int fetchSize = 10000;

    @Value("${rdf.dataset.taxref.name:http://taxref.mnhn.fr/lod/}")
    private String name;

    @Value("${rdf.dataset.taxref.query:classpath:sparql/taxon.sparql}")
    private String queryFile;

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
}