package net.sumaris.core.vo.referential.conversion;

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
import net.sumaris.core.dao.technical.jpa.IFetchOptions;

@Data
@Builder
public class WeightLengthConversionFetchOptions implements IFetchOptions {

    public static final WeightLengthConversionFetchOptions DEFAULT = WeightLengthConversionFetchOptions.builder().build();

    @Builder.Default
    private boolean withLocation = false;

    @Builder.Default
    private boolean withRectangleLabels = false;

    @Builder.Default
    private boolean withLengthPmfmIds = false;
}
