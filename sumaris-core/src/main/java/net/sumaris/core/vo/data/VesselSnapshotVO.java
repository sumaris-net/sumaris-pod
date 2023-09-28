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
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.data.IWithRecorderPersonEntity;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.referential.LocationVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@FieldNameConstants
@Document(indexName = VesselSnapshotVO.INDEX)
public class VesselSnapshotVO implements IDataVO<Integer>,
        IWithRecorderPersonEntity<Integer, PersonVO>, IRootDataVO<Integer> {

    public static final String INDEX = "vessel_snapshot";

    @Id
    @Field(type = FieldType.Integer)
    private Integer id;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Text)
    private String exteriorMarking;

    @Field(type = FieldType.Text)
    private String registrationCode;

    @Field(type = FieldType.Text)
    private String intRegistrationCode;

    @Field(type = FieldType.Integer)
    private Integer administrativePower;
    private Double lengthOverAll;
    private Double grossTonnageGrt;
    private Double grossTonnageGt;
    private LocationVO basePortLocation;
    private LocationVO registrationLocation;
    private String comments;


    @Field(type=FieldType.Date, format = DateFormat.basic_date_time_no_millis)
    private Date startDate;

    @Field(type=FieldType.Date, format = DateFormat.basic_date_time_no_millis)
    private Date endDate;

    @Field(type=FieldType.Date, format = DateFormat.basic_date_time)
    private Date creationDate;

    @Field(type=FieldType.Date, format = DateFormat.basic_date_time)
    private Date updateDate;

    @Field(type=FieldType.Date, format = DateFormat.basic_date_time)
    private Date controlDate;

    @Field(type=FieldType.Date, format = DateFormat.basic_date_time)
    private Date validationDate;

    @Field(type=FieldType.Date, format = DateFormat.basic_date_time)
    private Date qualificationDate;

    private String qualificationComments;
    private Integer qualityFlagId;
    private DepartmentVO recorderDepartment;
    private PersonVO recorderPerson;
    private ProgramVO program;

    private List<MeasurementVO> measurements;
    private Map<Integer, String> measurementValues;

    private ReferentialVO vesselType;
    private Integer vesselStatusId;

}
