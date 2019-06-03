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
import net.sumaris.core.model.data.IWithRecorderPersonEntity;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.referential.ReferentialVO;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
public class SampleVO implements  IRootDataVO<Integer>,
        IWithRecorderPersonEntity<Integer, PersonVO> {

    public static final String PROPERTY_SAMPLE_DATE = "sampleDate";
    public static final String PROPERTY_OPERATION = "operation";

    private Integer id;
    private String comments;
    private Date creationDate;
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
    private ReferentialVO matrix;
    private ReferentialVO taxonGroup;
    private ReferentialVO taxonName;

    private SampleVO parent;
    private Integer parentId;
    private List<SampleVO> children;

    private BatchVO batch;
    private Integer batchId;
    private OperationVO operation;
    private Integer operationId;

    private List<MeasurementVO> measurements; // sample_measurement
    private Map<Integer, String> measurementValues; // sample_measurement

    public String toString() {
        return new StringBuilder().append("SampleVO(")
                .append("id=").append(id)
                .append(",label=").append(label)
                .append(")").toString();
    }
}
