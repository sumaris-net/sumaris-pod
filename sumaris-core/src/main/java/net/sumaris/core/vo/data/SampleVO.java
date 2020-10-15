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
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.referential.TaxonNameVO;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@FieldNameConstants
@EqualsAndHashCode
public class SampleVO implements IRootDataVO<Integer>{
    @EqualsAndHashCode.Exclude
    private Integer id;
    private String comments;
    private Date creationDate;
    @EqualsAndHashCode.Exclude
    private Date updateDate;
    private Date controlDate;
    private Date validationDate;
    private Date qualificationDate;
    private String qualificationComments;
    private Integer qualityFlagId;
    private DepartmentVO recorderDepartment;
    private PersonVO recorderPerson;

    private ProgramVO program;

    private String label;
    private Date sampleDate;
    private Integer rankOrder;
    private Integer individualCount;
    private Double size;
    private String sizeUnit;

    @EqualsAndHashCode.Exclude
    private ReferentialVO matrix;
    private Integer matrixId;

    private ReferentialVO taxonGroup;
    private TaxonNameVO taxonName;

    @EqualsAndHashCode.Exclude
    private SampleVO parent;
    private Integer parentId;

    private List<SampleVO> children;

    @EqualsAndHashCode.Exclude
    private BatchVO batch;
    private Integer batchId;

    @EqualsAndHashCode.Exclude
    private OperationVO operation;
    private Integer operationId;

    @EqualsAndHashCode.Exclude
    private LandingVO landing;
    private Integer landingId;

    private Map<Integer, String> measurementValues; // = sample_measurement  (from a map)
    private List<MeasurementVO> measurements; // = sample_measurement (from a list)

    public String toString() {
        return new StringBuilder().append("SampleVO(")
                .append("id=").append(id)
                .append(",label=").append(label)
                .append(")").toString();
    }
}
