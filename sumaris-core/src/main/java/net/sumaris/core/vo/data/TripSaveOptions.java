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

import io.leangen.graphql.annotations.GraphQLIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.sumaris.core.dao.technical.jpa.ISaveOptions;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripSaveOptions implements ISaveOptions {

    public static TripSaveOptions DEFAULT = TripSaveOptions.builder().build();

    public static TripSaveOptions LANDED_TRIP = TripSaveOptions.builder()
        .withOperationGroup(true) // Enable operation group
        .withLanding(false)
        .withOperation(false)
        .build();

    public static TripSaveOptions defaultIfEmpty(TripSaveOptions options) {
        return options != null ? options : DEFAULT;
    }

    @Builder.Default
    private Boolean withOperation = false;

    @Builder.Default
    private Boolean withOperationGroup = false;

    @Builder.Default
    private Boolean withLanding = false;

    @Builder.Default
    private Boolean withExpectedSales = true;

    @GraphQLIgnore
    private Integer landingId;

    @GraphQLIgnore
    private Integer observedLocationId;
}
