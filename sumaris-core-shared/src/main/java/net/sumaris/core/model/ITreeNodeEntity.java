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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import lombok.NonNull;
import org.apache.commons.collections4.CollectionUtils;

import java.io.Serializable;
import java.util.List;

public interface ITreeNodeEntity<ID extends Serializable, E extends IEntity<ID>> extends IEntity<ID> {

    interface Fields {
        String PARENT = "parent";
        String CHILDREN = "children";
    }

    E getParent();

    void setParent(E parent);

    List<E> getChildren();

    void setChildren(List<E> children);

    @JsonIgnore
    default boolean hasChildren() {
        return CollectionUtils.isNotEmpty(getChildren());
    }

    @JsonIgnore
    default boolean hasParent() {
        return getParent() != null;
    }

    @JsonIgnore
    default <T extends ITreeNodeEntity<ID, E>> void addChildren(@NonNull E child) {
        if (child instanceof ITreeNodeEntity) ((ITreeNodeEntity<ID, E>) child).setParent((E) this);
        if (this.getChildren() == null) {
            setChildren(Lists.newArrayList((E) child));
        }
        else {
            getChildren().add((E) child);
        }
    }
}
