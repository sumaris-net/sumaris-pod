package net.sumaris.core.dao;

/*-
 * #%L
 * SUMARiS :: Sumaris Server Core
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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

import com.google.common.base.Preconditions;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;

/**
 * Fixtures for the local db.
 * 
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 * @since 1.0
 */
public class DatabaseFixtures {

	public String getSchemaVersion() {
		return "1.0";
	}

	public String getSchemaVersionIfUpdate() {
		return "1.0";
	}

	public int getUserProfileObserver() {
		return 2;
	}

	public String getPersonEmail(int index) {
		Preconditions.checkArgument(index >= 0);
		switch (index) {
			case 0:
				return "benoit.lavenier@e-is.pro";
			case 1:
				return "obs@sumaris.net";
			case 2:
				return "demo@sumaris.net";
			case 3:
				return "disable@sumaris.net";

			default:
				return "no-reply@sumaris.net";
		}
	}


	public Integer getVesselId(int index) {
		Preconditions.checkArgument(index >= 0);
		switch (index) {
			case 0:
				return 1;

			default:
				return 1;
		}
	}

	public Integer getTripId(int index) {
		Preconditions.checkArgument(index >= 0);
		switch (index) {
			case 0:
				return 1;

			default:
				return 1;
		}
	}


	public Integer getObservedLocationId(int index) {
		Preconditions.checkArgument(index >= 0);
		switch (index) {
			case 0:
				return 1;

			default:
				return 1;
		}
	}

	public Integer getSampleId(int index) {
		Preconditions.checkArgument(index >= 0);
		switch (index) {
			case 0:
				return 1;

			default:
				return 1;
		}
	}

	public Integer getBatchId(int index) {
		Preconditions.checkArgument(index >= 0);
		switch (index) {
			case 0:
				return 1;

			default:
				return 1;
		}
	}

	public Integer getLocationPortId(int index) {
		Preconditions.checkArgument(index >= 0);
		switch (index) {
			case 0:
				return 1;

			default:
				return 1;
		}
	}

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
			case 1:
				return 2;
			case 2:
				return 3; // User without any data in the test DB
			default:
				return 1;
		}
	}

	public Integer getPersonIdNoData() {
		return getPersonId(2);
	}

	public Integer getMetierIdForOTB(int index) {
		Preconditions.checkArgument(index >= 0);
		switch (index) {
			case 0:
				return 5;
			case 1:
				return 6;

			default:
				return 1;
		}
	}


	public Integer getMatrixIdForIndividual() {
		return 2; // INDIV
	}

	public Integer getTaxonGroupFAOId(int index) {
		Preconditions.checkArgument(index >= 0);
		return 1001 + index;
	}



    public ProgramVO getDefaultProgram() {
		ProgramVO program = new ProgramVO();
		program.setId(1);
		program.setLabel("SUMARiS");
		return program;
	}

	/* -- PMFM -- */

	public Integer getPmfmBatchWeight() {
		return 50;
	}

	public Integer getPmfmSampleTagId() {
		return 82;
	}

	public Integer getPmfmSampleIsDead() {
		return 94;
	}

	/* -- product -- */

	public Integer getProductId(int index) {
		Preconditions.checkArgument(index >= 0);
		switch (index) {
			case 0:
				return 1;

			default:
				return 1;
		}
	}
}
