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

package net.sumaris.server.http.graphql.technical;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeArgumentNotInBoundException;
import io.leangen.graphql.metadata.exceptions.TypeMappingException;
import io.leangen.graphql.metadata.strategy.type.TypeTransformer;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class DefaultTypeTransformer implements TypeTransformer {
    private final AnnotatedType rawReplacement;
    private final AnnotatedType unboundedReplacement;
    private final Map<Type, AnnotatedType> unboundedReplacements = new HashMap<>();

    public DefaultTypeTransformer(boolean replaceRaw, boolean replaceUnbounded) {
        AnnotatedType replacement = GenericTypeReflector.annotate(Object.class);
        this.rawReplacement = replaceRaw ? replacement : null;
        this.unboundedReplacement = replaceUnbounded ? replacement : null;
    }

    public DefaultTypeTransformer(AnnotatedType rawReplacement, AnnotatedType unboundedReplacement) {
        this.rawReplacement = rawReplacement;
        this.unboundedReplacement = unboundedReplacement;
    }

    public DefaultTypeTransformer addUnboundedReplacement(Type type, Type unboundedReplacement)  {
        this.unboundedReplacements.put(type, GenericTypeReflector.annotate(unboundedReplacement));
        return this;
    }

    public AnnotatedType transform(AnnotatedType type) throws TypeMappingException {
        AnnotatedType unboundedReplacement = this.unboundedReplacements.get(type.getType());
        if (unboundedReplacement != null) {
            return ClassUtils.eraseBounds(type, unboundedReplacement);
        }
        try {
            type = ClassUtils.eraseBounds(type, this.unboundedReplacement);
            return ClassUtils.completeGenerics(type, this.rawReplacement);
        } catch (TypeArgumentNotInBoundException e) {
            throw e;
        }
    }
}
