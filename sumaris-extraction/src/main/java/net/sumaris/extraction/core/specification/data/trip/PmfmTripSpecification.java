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
public interface PmfmTripSpecification extends RdbSpecification {
    String FORMAT = "PMFM_TRIP";
    String VERSION_1_0 = "1.0";

    String[] SHEET_NAMES = {
            TR_SHEET_NAME,
            HH_SHEET_NAME,
            SL_SHEET_NAME,
            HL_SHEET_NAME
    };
}
