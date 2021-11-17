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

package net.sumaris.rdf.core.service;

import net.sumaris.rdf.core.util.RdfFormat;
import org.apache.jena.rdf.model.Model;

import javax.annotation.Nullable;

public interface RdfModelService {

    boolean isLocalIri(String iri);

    Model get(String iri, @Nullable RdfFormat format);

    /**
     * Create an union model, from models loaded by IRI
     * @param iris
     * @param format
     * @return
     */
    Model union(String[] iris, @Nullable RdfFormat format);

    byte[] convert(Model model, @Nullable RdfFormat targetFormat);

    byte[] convert(String iris, @Nullable RdfFormat sourceFormat, RdfFormat targetFormat);

    byte[] unionThenConvert(String[] iris, @Nullable RdfFormat sourceFormat, RdfFormat targetFormat);
}
