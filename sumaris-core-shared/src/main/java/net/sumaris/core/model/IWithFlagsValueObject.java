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

import net.sumaris.core.util.Flags;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public interface IWithFlagsValueObject<ID extends Serializable> extends IValueObject<ID> {

    interface Fields extends IEntity.Fields {
        String FLAGS = "flags";
    }

    static <T extends IWithFlagsValueObject<?>> List<T> collectHasFlag(Collection<T> items, int flag) {
        return items.stream()
            .filter(item -> item.hasFlag(flag))
            .collect(Collectors.toList());
    }

    static <T extends IWithFlagsValueObject<?>> List<T> collectMissingFlag(Collection<T> items, int flag) {
        return items.stream()
            .filter(item -> item.hasNotFlag(flag))
            .collect(Collectors.toList());
    }

    int getFlags();

    void setFlags(int flags);

    default boolean hasFlag(int flag) {
        return Flags.hasFlag(getFlags(), flag);
    }

    default boolean hasNotFlag(int flag) {
        return Flags.hasNotFlag(getFlags(), flag);
    }

    default void addFlag(int flag) {
        this.setFlags(Flags.addFlag(getFlags(), flag));
    }

    default void removeFlag(int flag) {
        this.setFlags(Flags.removeFlag(getFlags(), flag));
    }
}
