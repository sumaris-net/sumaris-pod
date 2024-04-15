package net.sumaris.core.vo.data.vessel;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2024 SUMARiS Consortium
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
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.data.IDataEntity;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import net.sumaris.core.vo.referential.LocationVO;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import javax.persistence.*;
import java.util.Date;

@Getter
@Setter
@FieldNameConstants
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Document(indexName = VesselOwnerVO.INDEX, createIndex = false)
@Setting(settingPath = "settings/whitespace-analyzer.json")
public class VesselOwnerVO {

    @GraphQLIgnore
    public static final String INDEX = "vessel_owner";

    @Id
    private Integer id;

    @Field(type = FieldType.Text, fielddata = true, searchAnalyzer = "whitespace_analyzer")
    private String registrationCode;

    @Field(type = FieldType.Text, fielddata = true)
    private String lastName;

    @Field(type = FieldType.Text, fielddata = true)
    private String firstName;

    @Field(type = FieldType.Text, fielddata = true)
    private String street;

    @Field(type = FieldType.Text, fielddata = true, searchAnalyzer = "whitespace_analyzer")
    private String zipCode;

    @Field(type = FieldType.Text, fielddata = true)
    private String city;

    @EqualsAndHashCode.Include
    @Field(type = FieldType.Long)
    private Date dateOfBirth;

    @EqualsAndHashCode.Include
    @Field(type = FieldType.Long)
    private Date retirementDate;

    @EqualsAndHashCode.Include
    @Field(type = FieldType.Long)
    private Date activityStartDate;

    @Field(type = FieldType.Text, fielddata = true)
    private String phoneNumber;

    @Field(type = FieldType.Text, fielddata = true)
    private String mobileNumber;

    @Field(type = FieldType.Text, fielddata = true)
    private String faxNumber;

    @Field(type = FieldType.Text, fielddata = true)
    private String email;

    @Field(type = FieldType.Long)
    private Date updateDate;

    @Field(type = FieldType.Nested)
    private LocationVO countryLocation;

    @Field(type = FieldType.Nested)
    private ProgramVO program;

}
