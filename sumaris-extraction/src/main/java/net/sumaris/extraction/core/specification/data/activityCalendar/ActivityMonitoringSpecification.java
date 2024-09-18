package net.sumaris.extraction.core.specification.data.activityCalendar;

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

public interface ActivityMonitoringSpecification {
    String FORMAT = "ACTIMONIT";
    String VERSION = "1.0.0";

    String AM_RAW_SHEET_NAME = "AM_RAW";
    String AM_SHEET_NAME = "AM";

    String COLUMN_PROJECT = "project";

    String COLUMN_YEAR = "year";
    String COLUMN_REGISTRATION_LOCATION_LABEL = "registration_location_label";
    String COLUMN_BASE_PORT_LOCATION_LABEL = "base_port_location_label";
    String COLUMN_VESSEL_CODE = "vessel_registration_code";
    String COLUMN_OBSERVER = "observer";

    String COLUMN_MONTH_PREFIX = "month";

    String[] SHEET_NAMES = {AM_SHEET_NAME};

    // For DEBUG only
    String[] SHEET_NAMES_DEBUG = {
            AM_RAW_SHEET_NAME,
            AM_SHEET_NAME
    };
}
