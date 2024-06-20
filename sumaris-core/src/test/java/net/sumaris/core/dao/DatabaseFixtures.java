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

	public int getUserProfileSupervisor() {
		return 3;
	}

	public String getPersonEmail(int index) {
		Preconditions.checkArgument(index >= 0);
		return switch (index) {
			case 0 -> "benoit.lavenier@e-is.pro";
			case 1 -> "obs@sumaris.net";
			case 2 -> "demo@sumaris.net";
			case 3 -> "disable@sumaris.net";
			default -> "no-reply@sumaris.net";
		};
	}


	public Integer getVesselId(int index) {
		Preconditions.checkArgument(index >= 0);
		return switch (index) {
			case 0 -> 1;
			case 1 -> 2;
			case 2 -> 3;
			case 3 -> 4;
			default -> 1;
		};
	}

	public Integer getScientificVesselId() {
		return 4;
	}

	public String getVesselRegistrationCode(int index) {
		Preconditions.checkArgument(index >= 0);
		switch (index) {
			case 0:
				return "FRA000851751";
			default:
				return null;
		}
	}

	public Integer getVesselOwnerId(int index) {
		Preconditions.checkArgument(index >= 0);
		return switch (index) {
			case 0 -> 1;
			case 1 -> 2;
			default -> 2;
		};
	}

	public Integer getTripId(int index) {
		Preconditions.checkArgument(index >= 0);
		switch (index) {
			case 0:
				return 1; // Trip with batches
			case 1:
				return 2; // Trip with samples (survival test)

			default:
				return 1;
		}
	}

	public Integer getTripIdWithBatches() {
		return 1;
	}

	public Integer getTripIdWithSubGears() {
		return 70;
	}

	public Integer getOperationId(int index) {
		Preconditions.checkArgument(index >= 0);
		switch (index) {
			case 0:
				return 1;
			case 1:
				return 2;

			default:
				return 1;
		}
	}

	public Integer getOperationIdWithBatches() {
		return 2;
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

	public Integer getLandingId(int index) {
		Preconditions.checkArgument(index >= 0);
		switch (index) {
			case 0:
				return 1;

			default:
				return 1;
		}
	}

	public Integer getSampleIdWithImages() {
		return 4; // ADAP-CONTROLE sample, with image
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

	public String getSampleLabel(int index) {
		Preconditions.checkArgument(index >= 0);
		// See ADAP-CONTROLE data
		switch (index) {
			case 0:
			default:
				return "01002";
		}
	}
	public String getSampleTagId(int index) {
		Preconditions.checkArgument(index >= 0);
		// See SIH-OBSBIO data
		switch (index) {
			case 0:
				return "20LEUCCIR001-0001";
			case 1:
			default:
				return "20LEUCCIR001-0002";
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

	public Integer getActivityCalendarId(int index) {
		Preconditions.checkArgument(index >= 0);
		switch (index) {
			case 0:
				return 1; // on vessel 1

			default:
				return 1;
		}
	}

	/* -- Referential -- */

	public Integer getGearId(int index) {
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
				return 10;
			case 1:
				return 11;

			default:
				return 10;
		}
	}

	public Integer getLocationCountryId(int index) {
		Preconditions.checkArgument(index >= 0);
		switch (index) {
			case 0:
				return 1; // FRA
			case 1:
				return 2; // GBR
			case 2:
				return 3; // BEL
			case 3:
				return 4; // SEY

			default:
				return 1;

		}
	}

	public String[] getRectangleLabels() {
		return new String[]{"65F1", "65F2", "65F3", "65F4","65F5"};
	}

	public int getRectangleId(int index) {
        return switch (index) {
            case 0 -> 110;  // 65F1
            case 1 -> 111; // 65F2
            case 2 -> 112; // 65F3
            case 3 -> 113; // 65F4
            case 4 -> 114; // 65F5

            default -> 110;
        };
	}

	public Integer getDepartmentId(int index) {
		Preconditions.checkArgument(index >= 0);
		switch (index) {
			case 0:
				return 1;
			case 1:
				return 4;
			case 2:
				return 2;
			default:
				return 1;
		}
	}

	public Integer getPersonId(int index) {
		Preconditions.checkArgument(index >= 0);
		switch (index) {
			case 0:
				return 1; // B. Lavenier
			case 1:
				return getPersonIdObserver();
			case 2:
				return getPersonIdManager();
			case 3:
				return getPersonIdInactive();
			case 4:
				return getPersonIdAdmin();
			default:
				return 1;
		}
	}
	public Integer getPersonIdInactive() {
		return 4; // Inactive user
	}

	public Integer getPersonIdNoData() {
		return 4; // No data user (and no program privilege)
	}

	public Integer getPersonIdNoPrivilege() {
		return 6; // Guest user
	}

	public Integer getPersonIdObserver() {
		return 2; // Observer
	}

	public Integer getPersonIdManager() {
		return 3; // Manager
	}

	public Integer getPersonIdAdmin() {
		return 5; // Admin
	}


	public Integer getPersonIdViewer() {
		return 7; // Viewer
	}

	public Integer getPersonIdValidator() {
		return 8; // Validator
	}
	public Integer getPersonIdQualifier() {
		return 9; // Qualifier
	}

	public Integer getMetierIdForOTB(int index) {
		Preconditions.checkArgument(index >= 0);
		switch (index) {
			case 0:
				return 5;
			case 1:
				return 17;

			default:
				return 38;
		}
	}

	public Integer getMetierIdForFPO(int index) {
		Preconditions.checkArgument(index >= 0);
		switch (index) {
			case 0:
				return 6;
			case 1:
				return 16;
			case 2:
				return 55;

			default:
				return 92;
		}
	}

	public Integer getMatrixIdForIndividual() {
		return 2; // INDIV
	}

	public Integer getTaxonGroupFAOId(int index) {
		Preconditions.checkArgument(index >= 0);
		return 1001 + index;
	}

	public Integer getTaxonGroupMNZ() {
		return 1122;
	}

	public Integer getTaxonGroupCOD() {
		return 1161;
	}

	public Integer getTaxonGroupIdWithManyTaxonName() {
		return 1122; // MNZ - Baudroie
	}

	public Integer getReferenceTaxonId(int index) {
		Preconditions.checkArgument(index >= 0);
		return 1001 + index;
	}

	public Integer getReferenceTaxonIdCOD() {
		return 1061; // Gadus Morhua
	}

    public ProgramVO getDefaultProgram() {
		ProgramVO program = new ProgramVO();
		program.setId(1);
		program.setLabel("SUMARiS");
		return program;
	}

	public ProgramVO getAuctionProgram() {
		ProgramVO program = new ProgramVO();
		program.setId(11);
		program.setLabel("ADAP-CONTROLE");
		return program;
	}

	public ProgramVO getWithSubGearsProgram() {
		ProgramVO program = new ProgramVO();
		program.setId(70);
		program.setLabel("APASE");
		return program;
	}

	public ProgramVO getActivityCalendarProgram() {
		ProgramVO program = new ProgramVO();
		program.setId(110);
		program.setLabel("SIH-ACTIFLOT");
		return program;
	}

	public ProgramVO getActivityCalendarPredocProgram() {
		ProgramVO program = new ProgramVO();
		program.setId(111);
		program.setLabel("SIH-ACTIPRED");
		return program;
	}

	public ProgramVO getDailyActivityCalendarProgram() {
		ProgramVO program = new ProgramVO();
		program.setId(30);
		program.setLabel("SIH-OBSDEB");
		return program;
	}


	/* -- PMFM -- */

	public Integer getPmfmBatchWeight() {
		return 50;
	}
	public Integer getPmfmBatchTotalLengthCm() {
		return 81;
	}

	public Integer getPmfmSampleTagId() {
		return 82;
	}

	public Integer getPmfmSampleIsDead() {
		return 94;
	}

	/* -- product -- */

	public Long getProductId(int index) {
		Preconditions.checkArgument(index >= 0);
		switch (index) {
			case 0:
				return 1L;

			default:
				return 1L;
		}
	}

    public String getAdminPubkey() {
		return "Hg8gVyHTNxidhupuPNePW4CjQKzaZz66Vzowgb553ZdB";
	}

	public String getObserverPubkey() {
		return "5rojwz7mTRFE9LCJXSGB2w48kcZtg7vM4SDQkN2s9GFe";
	}
}
