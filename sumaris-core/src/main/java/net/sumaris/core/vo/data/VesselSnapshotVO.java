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

import io.leangen.graphql.annotations.GraphQLIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.data.IWithRecorderPersonEntity;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.referential.LocationVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@FieldNameConstants
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Document(indexName = VesselSnapshotVO.INDEX, createIndex = false)
@Setting(settingPath = "settings/whitespace-analyzer.json")
public class VesselSnapshotVO implements IDataVO<Integer>,
        IWithRecorderPersonEntity<Integer, PersonVO>, IRootDataVO<Integer> {

    @GraphQLIgnore
    public static final String INDEX = "vessel_snapshot";

    @Id
    private Integer vesselFeaturesId; // = VesselFeatures.ID = the unique key used by ElasticSearch indexation

    @GraphQLIgnore
    @EqualsAndHashCode.Include
    private Integer vesselId; // = Vessel.ID of original id, need by ElasticSearch indexation

    @Field(type = FieldType.Text, fielddata = true)
    private String name;

    @Field(type = FieldType.Text, fielddata = true, searchAnalyzer = "whitespace_analyzer")
    private String exteriorMarking;

    @Field(type = FieldType.Text, fielddata = true, searchAnalyzer = "whitespace_analyzer")
    private String registrationCode;

    @Field(type = FieldType.Text, fielddata = true, searchAnalyzer = "whitespace_analyzer")
    private String intRegistrationCode;

    private Integer administrativePower;
    private Double lengthOverAll;
    private Double grossTonnageGrt;
    private Double grossTonnageGt;
    @Field(type = FieldType.Nested)
    private LocationVO basePortLocation;
    @Field(type = FieldType.Nested)
    private LocationVO registrationLocation;
    @Field(type = FieldType.Text, index = false)
    private String comments;

    @EqualsAndHashCode.Include
    @Field(type = FieldType.Long)
    private Date startDate;
    @Field(type = FieldType.Long)
    private Date endDate;
    @Field(type = FieldType.Long)
    private Date creationDate;
    @Field(type = FieldType.Long)
    private Date updateDate;

    @Field(type = FieldType.Long, index = false)
    private Date controlDate;
    @Field(type = FieldType.Long, index = false)
    private Date validationDate;

    @Field(type = FieldType.Long, index = false)
    private Date qualificationDate;
    @Field(type = FieldType.Text, index = false)
    private String qualificationComments;
    private Integer qualityFlagId;
    private DepartmentVO recorderDepartment;
    private PersonVO recorderPerson;

    @Field(type = FieldType.Nested)
    private ProgramVO program;

    private List<MeasurementVO> measurements;
    private Map<Integer, String> measurementValues;

    @Field(type = FieldType.Nested)
    private ReferentialVO vesselType;
    private Integer vesselStatusId;

    @Override
    public Integer getId() {
        return vesselId;
    }

    @Override
    public void setId(Integer id) {
        this.vesselId = id;
    }
}
