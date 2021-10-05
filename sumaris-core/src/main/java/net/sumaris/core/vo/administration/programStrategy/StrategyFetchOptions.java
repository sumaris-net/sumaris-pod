package net.sumaris.core.vo.administration.programStrategy;

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

import lombok.*;
import net.sumaris.core.dao.technical.jpa.IFetchOptions;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class StrategyFetchOptions implements IFetchOptions {

    public static StrategyFetchOptions DEFAULT = StrategyFetchOptions.builder().build();

    public static StrategyFetchOptions nullToDefault(StrategyFetchOptions options) {
        if (options != null) return options;
        return DEFAULT;
    }

    /**
     * Fetch taxon names
     */
    @Builder.Default
    private boolean withTaxonNames = false;

    /**
     * Fetch taxon groups
     */
    @Builder.Default
    private boolean withTaxonGroups = false;

    /**
     * Fetch departments
     */
    @Builder.Default
    private boolean withDepartments = false;

    /**
     * Fetch gears
     */
    @Builder.Default
    private boolean withGears = false;

    /**
     * Compute PmfmStrategy (normalized entities)
     */
    @Builder.Default
    private boolean withPmfms = false;

    /**
     * Compute the denormalized PMFM, from PmfmStrategy
     */
    @Builder.Default
    private boolean withDenormalizedPmfms = false;



    /**
     * Fetch strategy for PSFM strategy
     */
    @Builder.Default
    private PmfmStrategyFetchOptions pmfmsFetchOptions = PmfmStrategyFetchOptions.DEFAULT;
}
