package net.sumaris.core.model.referential.taxon;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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

import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.dao.technical.model.annotation.EntityEnum;

import java.io.Serializable;
import java.util.Arrays;

@EntityEnum(entity = TaxonomicLevel.class, joinAttributes = {IEntity.Fields.ID})
public enum TaxonomicLevelEnum implements Serializable {

    FAMILY(17, "FAMILY"),
    GENUS(26, "GENUS"),
    SPECIES(28, "SPECIES"),
    SUBSPECIES(29, "SUBSPECIES");

    private Integer id;
    private String label;

    TaxonomicLevelEnum(Integer id, String label) {
        this.id = id;
        this.label = label;
    }

    public static TaxonomicLevelEnum valueOf(final int id) {
        return Arrays.stream(values())
            .filter(enumValue -> enumValue.id == id)
            .findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown TaxonomicLevelId: " + id));
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

    public String getLabel() {
        return this.label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

}
