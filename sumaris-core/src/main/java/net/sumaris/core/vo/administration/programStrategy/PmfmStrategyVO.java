package net.sumaris.core.vo.administration.programStrategy;

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
import net.sumaris.core.vo.referential.IReferentialVO;
import net.sumaris.core.vo.referential.PmfmVO;
import net.sumaris.core.vo.referential.ReferentialVO;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

@Data
public class PmfmStrategyVO implements IUpdateDateEntityBean<Integer, Date> {

    private Integer id;

    private String label;
    private String name;
    private Date updateDate;

    private String unit;
    private String type;

    private Double minValue;
    private Double maxValue;
    private Integer maximumNumberDecimals;
    private Double defaultValue;

    private Integer acquisitionNumber;
    private Boolean isMandatory;
    private Integer rankOrder;

    private String acquisitionLevel;
    private List<String> gears;

    List<ReferentialVO> qualitativeValues;
}
