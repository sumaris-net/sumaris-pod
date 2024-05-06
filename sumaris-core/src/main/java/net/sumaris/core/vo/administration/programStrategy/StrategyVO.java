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
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.vo.referential.IReferentialVO;
import net.sumaris.core.vo.referential.ReferentialVO;

import java.util.Date;
import java.util.List;

@Data
@FieldNameConstants
public class StrategyVO implements IReferentialVO<Integer>{

    private Integer id;
    private String label;
    private String name;
    private String description;
    private String comments;
    private String analyticReference;
    private Date updateDate;
    private Date creationDate;

    private Integer statusId;
    private Integer programId;

    private List<AppliedStrategyVO> appliedStrategies;

    private List<PmfmStrategyVO> pmfms;
    private List<DenormalizedPmfmStrategyVO> denormalizedPmfms;

    private List<StrategyDepartmentVO> departments;

    private List<Integer> gearIds;
    private List<ReferentialVO> gears;

    private List<Integer> taxonGroupIds;
    private List<TaxonGroupStrategyVO> taxonGroups;

    private List<Integer> taxonNameIds;
    private List<TaxonNameStrategyVO> taxonNames;


    private String entityName = "Strategy";
}
