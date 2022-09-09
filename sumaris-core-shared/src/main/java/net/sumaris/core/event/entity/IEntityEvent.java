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

import lombok.experimental.FieldNameConstants;

import java.io.Serializable;


public interface IEntityEvent<ID extends Serializable, V extends Serializable> {


    interface Fields {
        String OPERATION = "operation";
        String ENTITY_NAME = "entityName";
        String ID = "id";
        String _TYPE = "_type";
    }

    enum EntityEventOperation {
        INSERT,
        UPDATE,
        DELETE
    }

    EntityEventOperation getOperation();

    ID getId();

    void setId(ID id);

    String getEntityName();

    void setEntityName(String entityName);

    V getData();

    void setData(V data);

    String get_type();

    void set_type(String _type);
}
