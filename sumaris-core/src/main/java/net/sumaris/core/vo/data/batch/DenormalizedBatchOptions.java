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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.sumaris.core.model.referential.pmfm.QualitativeValueEnum;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.Dates;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DenormalizedBatchOptions {

    public static final DenormalizedBatchOptions DEFAULT = DenormalizedBatchOptions.builder().build();

    @Nonnull
    public static DenormalizedBatchOptions nullToDefault(@Nullable DenormalizedBatchOptions options) {
        return options != null ? options : DEFAULT;
    }

    @Builder.Default
    private boolean force = false; // Should recompute if denormalization already done ?

    @Builder.Default
    private boolean enableTaxonGroup = true;

    @Builder.Default
    private boolean enableTaxonName = true;

    @Builder.Default
    private boolean enableRtpWeight = false;

    @Builder.Default
    private boolean enableAliveWeight = true;

    @Builder.Default
    private boolean allowZeroWeightWithIndividual = true;

    private Integer[] taxonGroupIdsNoWeight;

    private Integer aliveWeightCountryLocationId; // Country location, used to find a alive weight conversion

    private Integer[] fishingAreaLocationIds; // Fishing areas used to find a weight length conversion

    private Date dateTime;

    @Builder.Default
    private Integer defaultLandingDressingId = QualitativeValueEnum.DRESSING_GUTTED.getId(); // /!\ in SIH Adagio, the denormalization job use WHL as default

    @Builder.Default
    private Integer defaultDiscardDressingId = QualitativeValueEnum.DRESSING_WHOLE.getId();

    @Builder.Default
    private Integer defaultLandingPreservationId = QualitativeValueEnum.PRESERVATION_FRESH.getId();

    @Builder.Default
    private Integer defaultDiscardPreservationId = QualitativeValueEnum.PRESERVATION_FRESH.getId();

    @Builder.Default
    private int maxRtpWeightDiffPct = 10; // 10% max pct between RTP weight and weight

    @JsonIgnore
    public int getMonth() {
        return dateTime != null ? Dates.getMonth(dateTime) + 1: null;
    }

    @JsonIgnore
    public int getYear() {
        return dateTime != null ? Dates.getYear(dateTime) : null;
    }

    /**
     * Get date, wuthout time. Useful for increase stability of cache keys
     * @return
     */
    @JsonIgnore
    public Date getDay() {
        return dateTime != null ? Dates.resetTime(dateTime) : null;
    }

    public DenormalizedBatchOptions clone() {
        return Beans.clone(this, DenormalizedBatchOptions.class);
    }

}
