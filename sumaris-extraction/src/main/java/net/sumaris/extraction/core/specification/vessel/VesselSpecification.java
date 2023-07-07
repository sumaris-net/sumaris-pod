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

package net.sumaris.extraction.core.specification.vessel;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public interface VesselSpecification {
    String FORMAT = "VESSEL";
    String VERSION_1_0 = "1.0";

    String VE_SHEET_NAME = "VE"; // Vessel

    String COLUMN_PROJECT = "project";
    String COLUMN_VESSEL_IDENTIFIER = "vessel_identifier";
    String COLUMN_REGISTRATION_START_DATE = "registration_start_date";
    String COLUMN_REGISTRATION_END_DATE = "registration_end_date";

    String[] SHEET_NAMES = {VE_SHEET_NAME};
}
