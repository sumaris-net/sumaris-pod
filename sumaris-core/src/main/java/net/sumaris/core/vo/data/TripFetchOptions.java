package net.sumaris.core.vo.data;

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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class TripFetchOptions implements IDataFetchOptions {

    public static final TripFetchOptions DEFAULT = TripFetchOptions.builder().build();

    public static final TripFetchOptions MINIMAL = TripFetchOptions.builder()
        .withRecorderDepartment(false)
        .withRecorderPerson(false)
        .withObservers(false)
        .build();

    public static final TripFetchOptions FULL_GRAPH = TripFetchOptions.builder()
        .withChildrenEntities(true)
        .withMeasurementValues(true)
        .withGears(true)
        .withSales(true)
        .withExpectedSales(true)
        .build();

    @Builder.Default
    private boolean withRecorderDepartment = true;

    @Builder.Default
    private boolean withRecorderPerson = true;

    @Builder.Default
    private boolean withObservers = true;

    @Builder.Default
    private boolean withChildrenEntities = false; // Important: should be disabled by default (see TripService or LandingService)

    @Builder.Default
    private boolean withMeasurementValues = false;

    @Builder.Default
    private boolean withGears = true;

    @Builder.Default
    private boolean withSales = true;

    @Builder.Default
    private boolean withExpectedSales = false; // Disable by default - fix #IMAGINE-651

    @Builder.Default
    private boolean withLanding = true;

    @Builder.Default
    private boolean withLocations = true;

    @Builder.Default
    private boolean withProgram = true;
}
