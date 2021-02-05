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
@EqualsAndHashCode
public class StrategyFetchOptions implements IFetchOptions {

    public static StrategyFetchOptions DEFAULT = StrategyFetchOptions.builder().build();

    public static StrategyFetchOptions nullToDefault(StrategyFetchOptions options) {
        if (options != null) return options;
        return DEFAULT;
    }

    /**
     * Will copy property from Pmfms into the PmfmStrategyVO.
     * If only Parameter, Method, Matrix, Fraction exists on PmfmStrategy, will denormalized into a list of all compatible Pmfms
     */
    @Builder.Default
    private boolean withPmfmStrategyInheritance = false;

    /**
     * Compute the PSFM strategy full name (with parameter, matrix, fraction and method names)
     */
    @Builder.Default
    private boolean withPmfmStrategyCompleteName = false;
}
