package net.sumaris.core.model.referential;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
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

import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.annotation.EntityEnum;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Validity Status
 *
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@EntityEnum(entity = ValidityStatus.class, resolveAttributes = {IEntity.Fields.ID})
public enum ValidityStatusEnum implements Serializable {

    INVALID(0),
    VALID(1),
    PENDING(2);

    public static ValidityStatusEnum valueOf(final int id) {
        return Arrays.stream(values())
            .filter(enumValue -> enumValue.id == id)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown ValidityStatusEnum: " + id));
    }

    private Integer id;

    ValidityStatusEnum(Integer id) {
        this.id = id;
    }

    /**
     * Returns the database row id
     *
     * @return int the id
     */
    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

}
