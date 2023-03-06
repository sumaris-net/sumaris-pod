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

package net.sumaris.core.vo.data.batch;

import lombok.Builder;
import lombok.Data;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;

@Data
@Builder
public class DenormalizedBatchOptions {

    public static final DenormalizedBatchOptions DEFAULT = DenormalizedBatchOptions.builder().build();

    public static DenormalizedBatchOptions nullToDefault(@Nullable DenormalizedBatchOptions options) {
        return options != null ? options : DEFAULT;
    }

    /**
     * Allow to create a new builder, from existing source options. Allow to construct new options,
     * without changing the original source
     * @param source
     * @return a new builder
     */
    public static DenormalizedBatchOptions.DenormalizedBatchOptionsBuilder toBuilder(@Nullable DenormalizedBatchOptions source) {
        source = nullToDefault(source);
        return DenormalizedBatchOptions.builder()
            .enableTaxonGroup(source.enableTaxonGroup)
            .enableTaxonName(source.enableTaxonName)
            .taxonGroupIdsNoWeight(source.taxonGroupIdsNoWeight)
            .roundWeightCountryLocationId(source.roundWeightCountryLocationId)
            .statisticalRectangleId(source.statisticalRectangleId);
    }

    @Builder.Default
    private boolean enableTaxonGroup = true;

    @Builder.Default
    private boolean enableTaxonName = true;

    private List<Integer> taxonGroupIdsNoWeight;

    private Integer roundWeightCountryLocationId; // Country location, used to find a round weight conversion

    private Integer statisticalRectangleId; // Fishing area used to find a weight length conversion

    private Date dateTime;

}
