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

package net.sumaris.core.vo.referential.conversion;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.dao.technical.model.IUpdateDateEntity;
import net.sumaris.core.dao.technical.model.IValueObject;
import net.sumaris.core.vo.referential.LocationVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.referential.TaxonGroupVO;

import java.util.Date;

@Data
@FieldNameConstants
public class RoundWeightConversionVO implements IUpdateDateEntity<Integer, Date>, IValueObject<Integer> {

    private Integer id;

    private Double conversionCoefficient;
    private Date startDate;
    private Date endDate;

    private String description;
    private String comments;

    private Integer statusId;
    private Date creationDate;
    private Date updateDate;

    private Integer taxonGroupId;
    private TaxonGroupVO taxonGroup;

    private Integer locationId;
    private LocationVO location;

    private Integer dressingId;
    private ReferentialVO dressing;

    private Integer preservingId;
    private ReferentialVO preserving;

    //private String[] rectangleLabels;

}