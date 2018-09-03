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
import net.sumaris.core.dao.technical.model.IUpdateDateEntityBean;
import net.sumaris.core.model.data.VesselFeatures;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.referential.ReferentialVO;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

@Data
public class VesselFeaturesVO implements IUpdateDateEntityBean<Integer, Date> {

    public static final String PROPERTY_START_DATE = VesselFeatures.PROPERTY_START_DATE;
    public static final String PROPERTY_EXTERIOR_MARKING = VesselFeatures.PROPERTY_EXTERIOR_MARKING;
    public static final String PROPERTY_NAME = VesselFeatures.PROPERTY_NAME;

    private Integer id;
    private String name;
    private String exteriorMarking;
    private Integer lengthOverAll;
    private Integer administrativePower;
    private ReferentialVO basePortLocation;
    private String comments;

    private Date startDate;
    private Date endDate;

    private Date creationDate;
    private Date updateDate;

    private Integer qualityFlagId;
    private DepartmentVO recorderDepartment;
    private PersonVO recorderPerson;

    // From parent Entity
    private Integer vesselId;
    private Integer vesselTypeId;

}
