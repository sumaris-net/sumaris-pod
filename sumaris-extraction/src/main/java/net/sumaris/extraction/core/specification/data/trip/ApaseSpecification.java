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

import net.sumaris.core.dao.referential.pmfm.PmfmSpecifications;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public interface ApaseSpecification extends RdbSpecification {
    String FORMAT = "APASE";
    String VERSION_1_0 = "1.0";

    String FG_SHEET_NAME = "FG"; // Fishing gear
    String CT_SHEET_NAME = "CT"; // Catch

    String COLUMN_SUB_GEAR_IDENTIFIER = "sub_gear_identifier";
    String COLUMN_SUB_GEAR_POSITION = "sub_gear_position";

    String[] SHEET_NAMES = {
        TR_SHEET_NAME,
        FG_SHEET_NAME,
        HH_SHEET_NAME,
        CT_SHEET_NAME,
        SL_RAW_SHEET_NAME,
        SL_SHEET_NAME,
        HL_SHEET_NAME
    };
}
