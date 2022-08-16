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

package net.sumaris.core.vo.administration.user;

import com.google.common.base.Preconditions;
import lombok.NonNull;
import net.sumaris.core.model.referential.StatusEnum;

public abstract class Persons {

    protected Persons() {
        // Helper class
    }

    public static boolean isTemporary(@NonNull PersonVO person) {
        Preconditions.checkNotNull(person.getStatusId());
        StatusEnum status = StatusEnum.valueOf(person.getStatusId());
        return StatusEnum.TEMPORARY.equals(status);
    }

    public static boolean isDisableOrDeleted(@NonNull PersonVO person) {
        Preconditions.checkNotNull(person.getStatusId());
        StatusEnum status = StatusEnum.valueOf(person.getStatusId());
        return (StatusEnum.DISABLE.equals(status) || StatusEnum.DELETED.equals(status));
    }

    public static boolean isEnableOrTemporary(@NonNull PersonVO person) {
        return !isDisableOrDeleted(person);
    }
}
