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

package net.sumaris.rdf.core.service.data;

import net.sumaris.rdf.core.model.IModelVisitor;
import org.apache.jena.rdf.model.Model;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface RdfIndividualService {

    /**
     * Register a model visitor, e.g. for add data to instances
     * @param visitor
     */
    void register(IModelVisitor<Model, RdfIndividualFetchOptions> visitor);

    Model getIndividuals(RdfIndividualFetchOptions options);


}
