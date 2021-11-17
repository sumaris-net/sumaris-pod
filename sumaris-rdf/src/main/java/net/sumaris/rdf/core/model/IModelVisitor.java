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

package net.sumaris.rdf.core.model;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

public interface IModelVisitor<M extends Model, O extends Object> {


    default boolean accept(Model model, String prefix, String namespace, O options) {
        return true;
    };

    /**
     * Visit a model
     * @param model
     * @param ns
     * @param schemaUri
     */
    default void visitModel(Model model, String ns, String schemaUri) {

    }

    /**
     * Visit a ont class
     * @param model
     * @param classUri
     */
    default void visitClass(Model model, Resource ontClass, Class clazz) {

    }

    /**
     * Visit an instance
     * @param model
     * @param classUri
     */
    default void visitIndividual(Model model, Resource instance, Class clazz) {

    }
}
