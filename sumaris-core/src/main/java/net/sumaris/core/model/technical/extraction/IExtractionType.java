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

package net.sumaris.core.model.technical.extraction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.referential.StatusEnum;

public interface IExtractionType<P extends IEntity<Integer>, D extends IEntity<Integer>>  {

    interface Fields extends IEntity.Fields {

        String ID = "id";
        String LABEL = "label";
        String NAME = "name";
        String STATUS_ID = "statusId";

        String FORMAT = "format";
        String VERSION = "version";
        String SHEET_NAMES = "sheetNames";

        String PARENT_ID = "parentId";
        String PARENT = "parent";

    }

    String LABEL_SEPARATOR = "-";

    Integer getId();

    /**
     * A unique label. Computed from '[format]-[id]'
     * @return
     */
    String getLabel();

    default String getName() {
        return null;
    }

    String getFormat();

    String getVersion();

    default Integer getParentId() {
        return null;
    }

    @JsonIgnore
    default IExtractionType<P, D> getParent() {
        return null;
    }

    String[] getSheetNames();

    default Integer getStatusId() {
        return StatusEnum.TEMPORARY.getId(); // private by default
    }

}
