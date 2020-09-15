package net.sumaris.core.extraction.vo;

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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import net.sumaris.core.model.data.IWithRecorderDepartmentEntity;
import net.sumaris.core.dao.technical.model.IValueObject;
import net.sumaris.core.vo.administration.user.DepartmentVO;

@Data
public class ExtractionTypeVO implements IValueObject<Integer>,
        IWithRecorderDepartmentEntity<Integer, DepartmentVO> {

    public static final String PROPERTY_SHEET_NAMES = "sheetNames";

    private Integer id;
    private String category;
    private String label;
    private String name;
    private String version;
    private String[] sheetNames;
    private Integer statusId;
    private Boolean isSpatial;

    @JsonIgnore
    private ExtractionRawFormatEnum rawFormat;

    private DepartmentVO recorderDepartment;

    public String getFormat() {
        return getLabel() != null ? getLabel().split("-")[0] : null;
    }
}
