package net.sumaris.extraction.core.specification.data.observedLocation;

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

public interface ObservedLocationSpecification {
    String FORMAT = "P03";
    String VERSION = "1.0";

    String OL_SHEET_NAME = "OBSERVATION";
    String VESSEL_SHEET_NAME = "OBSERVATION_NAVIRE";

    String CATCH_SHEET_NAME = "CAPTURE";
    String CATCH_INDIVIDUAL_SHEET_NAME = "CAPTURE_INDIVIDU";
    String CATCH_LOT_SHEET_NAME = "CAPTURE_LOT";

    String TRIP_SHEET_NAME = "MAREE";
    String TRIP_CALENDAR_SHEET_NAME = "CALENDRIER_MAREE";

    String OBSERVER_SHEET_NAME = "OBSERVATEUR";
    String SALE_SHEET_NAME = "VENTES";
    String SALE_PB_PACKET_SHEET_NAME = "VENTES_PB_PACKET";
    String VARIABLE_COST_SHEET_NAME = "COUT_VARIABLE";
    String OPERATION_SHEET_NAME = "OPERATION";

    String COLUMN_PROJECT = "project";

    String COLUMN_YEAR = "year";
    String COLUMN_INCLUDED_IDS = "included_ids";
    String COLUMN_LOCATION_LABEL = "location_label";
    String COLUMN_LOCATION_ID = "location_id";
    String COLUMN_VESSEL_CODE = "vessel_registration_code";
    String COLUMN_OBSERVER_NAME = "observer_name";
    String COLUMN_OBSERVER_ID = "observer_id";
    String COLUMN_RECORDER_NAME = "recorder_person_name";
    String COLUMN_RECORDER_ID = "recorder_person_id";

    String COLUMN_MONTH_PREFIX = "month";

    String[] SHEET_NAMES = {OL_SHEET_NAME, VESSEL_SHEET_NAME, CATCH_SHEET_NAME, CATCH_INDIVIDUAL_SHEET_NAME, CATCH_LOT_SHEET_NAME, TRIP_SHEET_NAME, TRIP_CALENDAR_SHEET_NAME, OBSERVER_SHEET_NAME, SALE_SHEET_NAME, SALE_PB_PACKET_SHEET_NAME, VARIABLE_COST_SHEET_NAME, OPERATION_SHEET_NAME};

    // For DEBUG only
    String[] SHEET_NAMES_DEBUG = {OL_SHEET_NAME, VESSEL_SHEET_NAME, CATCH_SHEET_NAME, CATCH_INDIVIDUAL_SHEET_NAME, CATCH_LOT_SHEET_NAME, TRIP_SHEET_NAME, TRIP_CALENDAR_SHEET_NAME, OBSERVER_SHEET_NAME, SALE_SHEET_NAME, SALE_PB_PACKET_SHEET_NAME, VARIABLE_COST_SHEET_NAME, OPERATION_SHEET_NAME};

}
