package net.sumaris.core.vo.data;

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

import lombok.Data;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.IValueObject;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.referential.location.LocationVO;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@FieldNameConstants
public class ExpectedSaleVO implements IEntity<Integer>, IValueObject<Integer> {

    private Integer id;
    private Date saleDate;
    private LocationVO saleLocation;
    private ReferentialVO saleType;

    @ToString.Exclude
    private TripVO trip;
    private Integer tripId;

    @ToString.Exclude
    private LandingVO landing;
    private Integer landingId;

    private List<MeasurementVO> measurements; // sale_measurement
    private Map<Integer, String> measurementValues; // sale_measurement

    private List<ProductVO> products;

}
