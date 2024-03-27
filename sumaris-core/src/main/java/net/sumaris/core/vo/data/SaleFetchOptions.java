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

package net.sumaris.core.vo.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.sumaris.core.vo.data.sample.SampleFetchOptions;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class SaleFetchOptions implements IDataFetchOptions {

    public static final SaleFetchOptions DEFAULT = SaleFetchOptions.builder().build();

    public static final SaleFetchOptions MINIMAL = SaleFetchOptions.builder()
        .withRecorderDepartment(false)
        .withRecorderPerson(false)
        .withVesselSnapshot(false)
        .build();

    public static final SaleFetchOptions FULL_GRAPH = SaleFetchOptions.builder()
        .withProgram(true)
        .withRecorderDepartment(true)
        .withRecorderPerson(true)
        .withVesselSnapshot(true)
        .withChildrenEntities(true)
        .withMeasurementValues(true)
        .withProducts(true)
        .withBatches(true)
        .build();

    public static SaleFetchOptions copy(IDataFetchOptions source) {
        return DataFetchOptions.copy(source, SaleFetchOptions.builder().build());
    }


    @Builder.Default
    private boolean withProgram = false;

    @Builder.Default
    private boolean withVesselSnapshot = false;

    @Builder.Default
    private boolean withRecorderDepartment = false;

    @Builder.Default
    private boolean withRecorderPerson = false;

    @Builder.Default
    private boolean withChildrenEntities = false; // Important: should be disabled by default (see TripService or LandingService)

    @Builder.Default
    private boolean withMeasurementValues = false;

    @Builder.Default
    private boolean withProducts = false;

    @Builder.Default
    private boolean withBatches = false;

    private boolean withFishingAreas = false;
}
