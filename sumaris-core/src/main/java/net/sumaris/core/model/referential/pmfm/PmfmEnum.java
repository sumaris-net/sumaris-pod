package net.sumaris.core.model.referential.pmfm;

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

import net.sumaris.core.model.annotation.EntityEnum;
import net.sumaris.core.model.annotation.IEntityEnum;

import java.io.Serializable;
import java.util.Arrays;

@EntityEnum(entity = Pmfm.class,
    configAttributes = Pmfm.Fields.ID,
    resolveAttributes = Pmfm.Fields.LABEL,
    required = false)
public enum PmfmEnum implements IEntityEnum, Serializable {

    SMALLER_MESH_GAUGE_MM(1, "SMALLER_MESH_GAUGE_MM"),
    HEADLINE_CUMULATIVE_LENGTH(12, "HEADLINE_CUMULATIVE_LENGTH"),

    HEADLINE_LENGTH(39, "HEADLINE_LENGTH"),
    BEAM_CUMULATIVE_LENGTH(13, "BEAM_CUMULATIVE_LENGTH"), // Adagio = BEAM_TOTAL_LENGTH
    BOTTOM_DEPTH_M(30, "BOTTOM_DEPTH_M"),
    BOTTOM_TEMP_C(32, "BOTTOM_TEMP_C"),
    GEAR_SPEED(9, "GEAR_SPEED"),
    GEAR_DEPTH_M(36, "GEAR_DEPTH_M"),
    GEAR_LABEL(120, "GEAR_LABEL"), // Libellé de l'engin
    NET_LENGTH(41, "NET_LENGTH"),
    DISCARD_OR_LANDING(90, "DISCARD_OR_LANDING"),

    LANDING_CATEGORY(436, "LANDING_CATEGORY"), // = PRODUCT_DESTINATION in Adagio

    CONVEYOR_BELT(20, "CONVEYOR_BELT"),
    NB_FISHERMEN(21, "NB_FISHERMEN"),
    NB_OPERATION(23, "NB_OPERATION"),
    NB_SAMPLING_OPERATION(24, "NB_SAMPLING_OPERATION"),
    MAIN_METIER(25, "MAIN_METIER"),
    RANDOM_SAMPLING_OPERATION(26, "RANDOM_SAMPLING_OPERATION"),

    SELECTIVITY_DEVICE(4, "SELECTIVITY_DEVICE"),
    SELECTIVITY_DEVICE_APASE(435, "SELECTIVITY_DEVICE_APASE"),
    ACOUSTIC_DETERRENT_DEVICE(5, "ACOUSTIC_DETERRENT_DEVICE"),
    SUBSTRATE_TYPE(31, "SUBSTRATE_TYPE"),
    SEA_STATE(33, "SEA_STATE"),
    TRIP_PROGRESS(34, "TRIP_PROGRESS"),
    SURVIVAL_SAMPLING_TYPE(35, "SURVIVAL_SAMPLING_TYPE"),
    CONTRACT_CODE(311, "CONTRACT_CODE"),

    LANDING_WEIGHT(50, "LANDING_WEIGHT"),
    SAND_STONES_WEIGHT_RANGE(51, "SAND_STONES_WEIGHT_RANGE"),
    BENTHOS_WEIGHT_RANGE(52, "BENTHOS_WEIGHT_RANGE"),
    ON_DECK_DATE_TIME(53, "ON_DECK_DATE_TIME"),
    SORTING_START_DATE_TIME(54, "SORTING_START_DATE_TIME"),
    SORTING_END_DATE_TIME(55, "SORTING_END_DATE_TIME"),

    SEX(80, "SEX"),
    TAG_ID(82, "TAG_ID"),
    LENGTH_TOTAL_CM(81, "LENGTH_TOTAL_CM"),
    LENGTH_CARAPACE_CM(84, "LENGTH_CARAPACE_CM"),
    LENGTH_CARAPACE_MM(85, "LENGTH_CARAPACE_MM"),


    SEGMENT_LENGTH_MM(86, "SEGMENT_LENGTH_MM"),
    LENGTH_MANTLE_CM(87, "LENGTH_MANTLE_CM"),

    LENGTH_TOTAL_MM(88, "LENGTH_TOTAL_MM"),

    HEIGHT_MM(89, "HEIGHT_MM"),

    LENGTH_LM_FORK_CM(441, "LENGTH_LM_FORK_CM"),

    LENGTH_FORK_CM(442, "LENGTH_FORK_CM"),

    LENGTH_PRE_SUPRA_CAUDAL_CM(443, "LENGTH_PRE_SUPRA_CAUDAL_CM"),

    DOM_HALF_CM(444, "DOM_HALF_CM"),

    WIDTH_CARAPACE_MM(445, "WIDTH_CARAPACE_MM"),

    BATCH_MEASURED_WEIGHT(91, "BATCH_MEASURED_WEIGHT"),
    BATCH_ESTIMATED_WEIGHT(92, "BATCH_ESTIMATED_WEIGHT"),
    BATCH_ESTIMATED_RATIO(178, "BATCH_ESTIMATED_RATIO"),
    BATCH_CALCULATED_WEIGHT(93, "BATCH_CALCULATED_WEIGHT"),
    BATCH_CALCULATED_WEIGHT_LENGTH(122, "BATCH_CALCULATED_WEIGHT_LENGTH"),
    BATCH_CALCULATED_WEIGHT_LENGTH_SUM(123, "BATCH_CALCULATED_WEIGHT_LENGTH_SUM"),
    BATCH_SORTING(176, "BATCH_SORTING"), // Adagio = 'SORTED' (id=200)
    CHILD_GEAR(400, "CHILD_GEAR"),
    CATCH_WEIGHT(57, "CATCH_WEIGHT"),
    DISCARD_WEIGHT(56, "DISCARD_WEIGHT"),
    BATCH_GEAR_POSITION(411, "BATCH_GEAR_POSITION"),

    IS_DEAD(94, "IS_DEAD"),
    DISCARD_REASON(95, "DISCARD_REASON"),
    DISCARD_TYPE(408, "DISCARD_TYPE"),
    IS_SAMPLING(409, "IS_SAMPLING"), // Lot est-il détaillé ou pas ?
    HAS_INDIVIDUAL_MEASURES(121, "HAS_INDIVIDUAL_MEASURES"),

    HULL_MATERIAL(440, "HULL_MATERIAL"), // Adagio HULL_MATERIAL (id=145)

    @Deprecated
    /**
     * @deprecated Use CONTRACT_CODE instead
     */
    SELF_SAMPLING_PROGRAM(28, "SELF_SAMPLING_PROGRAM"),

    CONTROL_TYPE(130, "CONTROL_TYPE"),

    SIZE_UNLI_CAT(141, "SIZE_UNLI_CAT"),
    PRESERVATION(150, "PRESERVATION"),
    DRESSING(151, "DRESSING"),
    SIZE_CATEGORY(174, "SIZE_CATEGORY"),
    TRAWL_SIZE_CAT(418, "TRAWL_SIZE_CAT"),
    PACKAGING(177, "PACKAGING"),

    TOTAL_PRICE(270, "TOTAL_PRICE"),
    AVERAGE_PACKAGING_PRICE(271, "AVERAGE_PACKAGING_PRICE"), // as generic average price
    AVERAGE_WEIGHT_PRICE(272, "AVERAGE_PRICE_WEI"),
    AVERAGE_VOLUME_PRICE(273, "AVERAGE_PRICE_VOL"),
    AVERAGE_UNIT_PRICE(274, "AVERAGE_PRICE_UNI"),
    AVERAGE_DOZEN_PRICE(275, "AVERAGE_PRICE_DOZ"),
    AVERAGE_HUNDRED_PRICE(276, "AVERAGE_PRICE_HUN"),
    AVERAGE_PIECES_PRICE(277, "AVERAGE_PRICE_PCS"),
    SALE_ESTIMATED_RATIO(278, "SALE_ESTIMATED_RATIO"),
    SALE_RANK_ORDER(279, "SALE_RANK_ORDER"),

    STRATEGY_LABEL(359, "STRATEGY_LABEL"),

    REFUSED_SURVEY(266, "REFUSED_SURVEY"),
    GPS_USED(188, "GPS_USED"),

    // ObsMer
    EMV_CATEGORY(437, "EMV_CATEGORY"),

    // ObsVente
    PETS(502, "PETS"),

    SALE_TYPE(503, "SALE_TYPE"),
    IS_OBSERVED(510, "IS_OBSERVED"),
    NON_OBSERVATION_REASON(511, "NON_OBSERVATION_REASON"),

    // Activity Calendar
    DURATION_AT_SEA_DAYS(449, "DURATION_AT_SEA_DAYS"),
    FISHING_DURATION_DAYS(450, "FISHING_DURATION_DAYS"),

    SURVEY_QUALIFICATION(446, "SURVEY_QUALIFICATION"),
    SURVEY_RELIABILITY(447, "SURVEY_QUALIFICATION"),
    ;

    public static PmfmEnum valueOf(final int id) {
        return Arrays.stream(values())
            .filter(enumValue -> enumValue.id == id)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown PmfmEnum: " + id));
    }

    private Integer id;
    private String label;

    PmfmEnum(Integer id, String label) {
        this.id = id;
        this.label = label;
    }

    /**
     * Returns the database row id
     *
     * @return int the id
     */
    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getLabel() {
        return this.label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
