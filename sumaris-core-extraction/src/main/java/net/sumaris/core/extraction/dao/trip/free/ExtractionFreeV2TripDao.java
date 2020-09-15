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

package net.sumaris.core.extraction.dao.trip.free;

import net.sumaris.core.extraction.dao.trip.ExtractionTripDao;
import net.sumaris.core.extraction.dao.trip.rdb.ExtractionRdbTripDao;
import net.sumaris.core.extraction.vo.ExtractionFilterVO;
import net.sumaris.core.extraction.vo.ExtractionRawFormatEnum;
import net.sumaris.core.extraction.vo.trip.free.ExtractionFreeV2ContextVO;
import net.sumaris.core.util.StringUtils;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public interface ExtractionFreeV2TripDao extends ExtractionRdbTripDao {
    String FREE2_FORMAT = StringUtils.underscoreToChangeCase(ExtractionRawFormatEnum.FREE2.getLabel());

    String TR_SHEET_NAME = "MAREE";
    String HH_SHEET_NAME = "OPERATION_PECHE";
    String GEAR_SHEET_NAME = "ENGINS";

    String SL_SHEET_NAME = "SL";
    String HL_SHEET_NAME = "HL";
    String CA_SHEET_NAME = "CA";

    ExtractionFreeV2ContextVO execute(ExtractionFilterVO filter);
}