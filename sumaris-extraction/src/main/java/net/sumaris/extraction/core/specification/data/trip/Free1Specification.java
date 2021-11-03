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

package net.sumaris.extraction.core.specification.data.trip;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public interface Free1Specification extends RdbSpecification {
    String FORMAT = "FREE";
    String VERSION_1 = "1";
    String[] SHEET_NAMES = {RdbSpecification.TR_SHEET_NAME, RdbSpecification.HH_SHEET_NAME, RdbSpecification.SL_SHEET_NAME, RdbSpecification.HL_SHEET_NAME};

    String COLUMN_FISHING_DATE = "fishing_date";
    String COLUMN_FISHING_DURATION = "fishing_duration";
    String COLUMN_SEX = CostSpecification.COLUMN_SEX;
}