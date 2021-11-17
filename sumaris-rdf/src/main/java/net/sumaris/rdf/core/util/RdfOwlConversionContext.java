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

import com.google.common.collect.Maps;
import org.apache.jena.ontology.OntResource;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class RdfOwlConversionContext {

    public Map<String, Class> URI_2_CLASS = new HashMap<>();
    public Map<String, Object> URI_2_OBJ_REF = Maps.newHashMap();
    public  Map<String, Function<OntResource, Object>> B2O_ARBITRARY_MAPPER = new HashMap<>();
    public  Map<String, Function<Object, OntResource>> O2B_ARBITRARY_MAPPER = new HashMap<>();
}
