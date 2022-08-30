package net.sumaris.core.model.technical.extraction.rdb;

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

import lombok.*;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.dao.technical.schema.DatabaseTableEnum;

import javax.persistence.*;
import java.io.Serializable;

@Getter
@Setter

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name = "p01_rdb_species_list")
public class ProductRdbSpeciesList implements Serializable, IEntity<Integer> {

    public static final DatabaseTableEnum TABLE = DatabaseTableEnum.P01_RDB_SPECIES_LIST;
    public static final String SHEET_NAME = "SL";

    public static final String COLUMN_SAMPLING_TYPE = ProductRdbStation.COLUMN_SAMPLING_TYPE;
    public static final String COLUMN_VESSEL_FLAG_COUNTRY = ProductRdbStation.COLUMN_VESSEL_FLAG_COUNTRY;
    public static final String COLUMN_LANDING_COUNTRY = ProductRdbStation.COLUMN_LANDING_COUNTRY;
    public static final String COLUMN_YEAR = ProductRdbStation.COLUMN_YEAR;
    public static final String COLUMN_PROJECT = ProductRdbStation.COLUMN_PROJECT;
    public static final String COLUMN_TRIP_CODE = ProductRdbStation.COLUMN_TRIP_CODE;
    public static final String COLUMN_STATION_NUMBER = ProductRdbStation.COLUMN_STATION_NUMBER;

    public static final String COLUMN_SPECIES = "species";
    public static final String COLUMN_SEX = "sex";
    public static final String COLUMN_CATCH_CATEGORY = "catch_category";
    public static final String COLUMN_LANDING_CATEGORY = "landing_category";
    public static final String COLUMN_COMMERCIAL_SIZE_CATEGORY_SCALE = "commercial_size_category_scale";
    public static final String COLUMN_COMMERCIAL_SIZE_CATEGORY = "commercial_size_category";
    public static final String COLUMN_SUBSAMPLING_CATEGORY = "subsampling_category";
    public static final String COLUMN_WEIGHT= "weight";
    public static final String COLUMN_SUBSAMPLING_WEIGHT = "subsampling_weight";
    public static final String COLUMN_LENGTH_CODE = "length_code";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "p01_rdb_species_list_seq")
    @SequenceGenerator(name = "p01_rdb_species_list_seq", sequenceName="p01_rdb_species_list_seq", allocationSize = 1)
    private Integer id;

    @Column(nullable = false, length = 2, name = COLUMN_SAMPLING_TYPE)
    private String samplingType;

    @Column(nullable = false, length = 3, name = COLUMN_LANDING_COUNTRY)
    private String landingCountry;

    @Column(nullable = false, length = 3, name = COLUMN_VESSEL_FLAG_COUNTRY)
    private String vesselFlagCountry;

    @Column(nullable = false, scale = 4, name = COLUMN_YEAR)
    private Integer year;

    @Column(nullable = false, name = COLUMN_PROJECT)
    private String project;

    @Column(nullable = false, name = COLUMN_TRIP_CODE, length = 50)
    private String tripCode;

    @Column(name = COLUMN_STATION_NUMBER, scale=6)
    private Integer stationNumber;

    // TODO: reduce length to 20 (or 4 ?)
    @Column(nullable = false, length = 100, name = COLUMN_SPECIES)
    private String species;

    @Column(nullable = false, length= 25, name = COLUMN_CATCH_CATEGORY)
    private String catchCategory;

    @Column(nullable = false, length = 25, name = COLUMN_LANDING_CATEGORY)
    private String landingCategory;

    @Column(length = 25, name = COLUMN_COMMERCIAL_SIZE_CATEGORY_SCALE)
    private String commercialSizeCategoryScale;

    @Column(length = 25, name = COLUMN_COMMERCIAL_SIZE_CATEGORY)
    private String commercialSizeCategory;

    @Column(length = 25, name = COLUMN_SUBSAMPLING_CATEGORY)
    private String subsamplingCategory;

    @Column(name = COLUMN_SEX , length=1)
    private String sex;

    @Column(nullable = false, scale = 12, precision = 2, name = COLUMN_WEIGHT)
    private Double weight;

    @Column( scale = 12, precision = 2, name = COLUMN_SUBSAMPLING_WEIGHT)
    private Double subsamplingWeight;

    @Column(length = 5, name = COLUMN_LENGTH_CODE)
    private String lengthCode;
}
