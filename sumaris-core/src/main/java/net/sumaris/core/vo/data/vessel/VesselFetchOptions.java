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

package net.sumaris.core.vo.data.vessel;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.sumaris.core.vo.data.IDataFetchOptions;

@Data
@Builder
@EqualsAndHashCode
public class VesselFetchOptions implements IDataFetchOptions {

    public static final VesselFetchOptions DEFAULT = VesselFetchOptions.builder().build();

    @Builder.Default
    private boolean withRecorderDepartment = false;

    @Builder.Default
    private boolean withRecorderPerson = false;

    @Builder.Default
    private boolean withChildrenEntities = false;

    @Builder.Default
    private boolean withVesselFeatures = true;

    @Builder.Default
    private boolean withVesselRegistrationPeriod = true;

    @Builder.Default
    private boolean withBasePortLocation = false;

    @Builder.Default
    private boolean withMeasurementValues = false;

    @Override
    public boolean isWithObservers() {
        return false;
    }


}
