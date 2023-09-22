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

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.sumaris.core.dao.technical.jpa.IFetchOptions;

@Data
@Builder
@EqualsAndHashCode
public class PmfmStrategyFetchOptions implements IFetchOptions {

    public static PmfmStrategyFetchOptions DEFAULT = PmfmStrategyFetchOptions.builder().build();

    public static PmfmStrategyFetchOptions nullToDefault(PmfmStrategyFetchOptions options) {
        if (options != null) return options;
        return DEFAULT;
    }

    /**
     * Compute the PSFM strategy full name (with parameter, matrix, fraction and method names)
     */
    @Builder.Default
    private boolean withCompleteName = false;

    @Builder.Default
    private boolean uniqueByPmfmId = false;

    @Builder.Default
    private boolean withGears = false;

    @Builder.Default
    private boolean withTaxonGroups = false;


}
