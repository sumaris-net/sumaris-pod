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

package net.sumaris.core.vo.data.sample;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.dao.technical.model.ITreeNodeEntity;
import net.sumaris.core.model.data.IWithRecorderPersonEntity;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.IRootDataVO;
import net.sumaris.core.vo.data.LandingVO;
import net.sumaris.core.vo.data.MeasurementVO;
import net.sumaris.core.vo.data.OperationVO;
import net.sumaris.core.vo.data.batch.BatchVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.referential.TaxonNameVO;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@ToString(onlyExplicitlyIncluded = true)
@FieldNameConstants
@EqualsAndHashCode
public class SampleVO implements IRootDataVO<Integer>,
    ITreeNodeEntity<Integer, SampleVO> {

    @EqualsAndHashCode.Exclude
    @ToString.Include
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

    @ToString.Include
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

}
