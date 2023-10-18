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

package net.sumaris.core.model;

import net.sumaris.core.util.Beans;

import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Objects;

public abstract class Entities {

    protected Entities() {
        // helper class does not instantiate
    }

    public static <ID extends Serializable, D extends Date, V extends IUpdateDateEntity<ID, D>, L extends Collection<V>> D maxUpdateDate(L entities) {
        return Beans.getStream(entities)
            .map(IUpdateDateEntity::getUpdateDate)
            .filter(Objects::nonNull)
            .max(Comparator.comparingLong(Date::getTime))
            .orElse(null);
    }

    public static <T extends IEntity<?>> void clearId(T entity) {
        entity.setId(null);
    }

    public static <T extends IUpdateDateEntity<?, ?>> void clearIdAndUpdateDate(T entity) {
        entity.setId(null);
        entity.setUpdateDate(null);
    }
}
