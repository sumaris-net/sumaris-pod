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

package net.sumaris.extraction.core;

import com.google.common.base.Preconditions;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;

/**
 * Fixtures for the local db.
 * 
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 * @since 1.0
 */
public class DatabaseFixtures {


	public Integer getDepartmentId(int index) {
		Preconditions.checkArgument(index >= 0);
		switch (index) {
			case 0:
				return 1;
			case 1:
				return 4;
			default:
				return 1;
		}
	}

	public Integer getPersonId(int index) {
		Preconditions.checkArgument(index >= 0);
		switch (index) {
			case 0:
				return 1;

			default:
				return 1;
		}
	}
	public String getRdbProductLabel(int index) {
		Preconditions.checkArgument(index >= 0);
		switch (index) {
			case 0:
			default:
				return "RDB-01";
		}
	}

	public String getProgramLabelForPmfmExtraction(int index) {
		Preconditions.checkArgument(index >= 0);
		switch (index) {
			case 0:
				return "SUMARiS";
			case 1:
				return "ADAP-MER";
			case 2:
				return "PIFIL";
			default:
				return "SUMARiS";
		}
	}

	public int getYearRawData() {
		return 2018;
	}


	public int getYearRdbProduct() {
		return 2012;
	}

	public int getTripIdByProgramLabel(String programLabel) {
		switch (programLabel) {
			case "APASE":
				return 70;
			case "ADAP-MER":
				return 100;
			default:
				throw new IllegalArgumentException("Add trip id for program " + programLabel);
		}
	}

}
