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

package net.sumaris.core.vo.referential.pmfm;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.referential.pmfm.Parameter;
import net.sumaris.core.vo.referential.ReferentialVO;

import java.util.List;

@Data
@FieldNameConstants
@EqualsAndHashCode(callSuper = true)
public class ParameterVO extends ReferentialVO {

    private String type;
    private Integer parameterGroupId;
    List<ReferentialVO> qualitativeValues;

    public ParameterVO() {
        this.setEntityName(Parameter.ENTITY_NAME);
    }
}
