package net.sumaris.extraction.core.specification.actimonit;

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

public interface MonitoringSpecification {
    String FORMAT = "ACTIMONIT";
    String VERSION = "1.0.0";

    String AM_SHEET_NAME = "AM";
    String RESULT_AM_SHEET_NAME = "RESULT_AM";

    String COLUMN_PROJECT = "projet";

    String COLUMN_YEAR = "year";
    String COLUMN_END_DATE = "END_DATE";
    String[] SHEET_NAMES = {AM_SHEET_NAME};

    // Time columns
    String COLUMN_AREA = "area";
    String COLUMN_MONTH = "month";

}
