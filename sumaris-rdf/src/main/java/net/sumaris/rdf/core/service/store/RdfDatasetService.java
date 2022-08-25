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

import net.sumaris.rdf.core.loader.INamedRdfLoader;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;

import java.util.Set;
import java.util.concurrent.Callable;

public interface RdfDatasetService {

    void registerNameModel(final INamedRdfLoader producer, final long maxStatements);

    void registerNamedModel(final String name, final Callable<Model> producer);

    void loadAllNamedModels(Dataset dataset, boolean replaceIfExists);

    /**
     * Construct a dataset for a query
     * @param query
     * @return a dataset
     */
    Dataset prepareDatasetForQuery(Query query);

    /**
     * Fill dataset
     * @return
     */
    void initDataset();

    /**
     * Get the dataset
     * @return
     */
    Dataset getDataset();

    String getProviderName();

    /**
     * Get named models
     * @return
     */
    Set<String> getModelNames();

    void loadModels(boolean replaceIfExists);
}
