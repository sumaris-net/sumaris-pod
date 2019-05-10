package net.sumaris.core.model.product.ices;

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
import net.sumaris.core.dao.technical.model.IDataEntity;
import net.sumaris.core.dao.technical.schema.DatabaseTableEnum;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@Table(name = "p01_ices_species_list")
public class ProductIcesSpeciesList implements Serializable, IDataEntity<Integer> {

    public static final DatabaseTableEnum TABLE = DatabaseTableEnum.P01_ICES_SPECIES_LIST;

    public static final String COLUMN_SAMPLING_TYPE = ProductIcesStation.COLUMN_SAMPLING_TYPE;
    public static final String COLUMN_VESSEL_FLAG_COUNTRY = ProductIcesStation.COLUMN_VESSEL_FLAG_COUNTRY;
    public static final String COLUMN_LANDING_COUNTRY = ProductIcesStation.COLUMN_LANDING_COUNTRY;
    public static final String COLUMN_YEAR = ProductIcesStation.COLUMN_YEAR;
    public static final String COLUMN_PROJECT = ProductIcesStation.COLUMN_PROJECT;
    public static final String COLUMN_TRIP_CODE = ProductIcesStation.COLUMN_TRIP_CODE;
    public static final String COLUMN_STATION_NUMBER = ProductIcesStation.COLUMN_STATION_NUMBER;

    public static final String COLUMN_SPECIES = "species";
    public static final String COLUMN_SEX = "sex";
    public static final String COLUMN_CATCH_CATEGORY = "catch_category";
    public static final String COLUMN_LANDING_CATEGORY = "landing_category";
    public static final String COLUMN_COMMERCIAL_SIZE_CATEGORY_SCALE = "comm_size_cat_scale";
    public static final String COLUMN_COMMERCIAL_SIZE_CATEGORY = "comm_size_cat";
    public static final String COLUMN_SUBSAMPLING_CATEGORY = "subsampling_category";
    public static final String COLUMN_WEIGHT= "weight";
    public static final String COLUMN_SUBSAMPLING_WEIGHT = "subsampling_weight";
    public static final String COLUMN_LENGTH_CODE = "length_code";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "p01_ices_species_list_seq")
    @SequenceGenerator(name = "p01_ices_species_list_seq", sequenceName="p01_ices_species_list_seq")
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
    private String commercialCategoryScale;

    @Column(length = 25, name = COLUMN_COMMERCIAL_SIZE_CATEGORY)
    private String commercialCategory;

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
