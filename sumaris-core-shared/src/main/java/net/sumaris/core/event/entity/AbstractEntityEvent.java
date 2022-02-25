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

package net.sumaris.core.event.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.sumaris.core.dao.technical.model.IValueObject;

import java.io.Serializable;

@Data
@AllArgsConstructor
public abstract class AbstractEntityEvent<ID extends Serializable, V extends Serializable>
        implements IEntityEvent<ID, V> {

    private final IEntityEvent.EntityEventOperation operation;

    private ID id;

    private String entityName;

    private V data;

    private String _type;

    protected AbstractEntityEvent(IEntityEvent.EntityEventOperation operation) {
        this.operation = operation;
    }

    protected AbstractEntityEvent(IEntityEvent.EntityEventOperation operation, ID id, String entityName, V data){
        this(operation, id, entityName, data, data != null ? data.getClass().getName() : null);
    }

}
