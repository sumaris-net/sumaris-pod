package net.sumaris.core.dao.technical.schema;

/*-
 * #%L
 * SUMARiS:: Core
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


import com.google.common.collect.Lists;

import java.util.List;

/**
 * All tables to synchronize.
 *
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 * @since 1.0
 */
public enum DatabaseTableEnum {

	// ICES format tables (v1)
	P01_ICES_LANDING, // CL
	P01_ICES_TRIP, // TR
	P01_ICES_STATION, // HH
	P01_ICES_SPECIES_LIST, // SL
	P01_ICES_SPECIES_LENGTH // HL
	;

	private final boolean association;

	DatabaseTableEnum() {
		this(false);
	}

	DatabaseTableEnum(boolean association) {
		this.association = association;
	}

	public boolean isAssociation() {
		return association;
	}

	public static DatabaseTableEnum[] getTables() {
		List<DatabaseTableEnum> result = Lists.newArrayList();
		for (DatabaseTableEnum table : values()) {
			if (!table.isAssociation()) {
				result.add(table);
			}
		}
		return result.toArray(new DatabaseTableEnum[result.size()]);
	}

}
