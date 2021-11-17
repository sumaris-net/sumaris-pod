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

package net.sumaris.extraction.core.specification.administration;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public interface StratSpecification {
    String FORMAT = "STRAT";
    String VERSION_1_0 = "1.0";

    String ST_SHEET_NAME = "ST"; // Strategy
    String SM_SHEET_NAME = "SM"; // Strategy Monitoring

    String COLUMN_PROJECT = "project";
    String COLUMN_STRATEGY = "strategy";
    String COLUMN_STRATEGY_ID = "strategy_id";
    String COLUMN_START_DATE = "start_date";
    String COLUMN_END_DATE = "end_date";

    String[] SHEET_NAMES = {ST_SHEET_NAME, SM_SHEET_NAME};
}
