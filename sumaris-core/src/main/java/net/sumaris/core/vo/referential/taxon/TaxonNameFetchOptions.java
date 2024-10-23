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

package net.sumaris.core.vo.referential.taxon;

import lombok.Builder;
import lombok.Data;
import net.sumaris.core.dao.technical.jpa.IFetchOptions;

@Data
@Builder
public class TaxonNameFetchOptions implements IFetchOptions {

    public static final TaxonNameFetchOptions DEFAULT = TaxonNameFetchOptions.builder().build();

    public static final TaxonNameFetchOptions FULL = TaxonNameFetchOptions.builder()
        .withParentTaxonName(true)
        .withTaxonomicLevel(true)
        .build();

    public static TaxonNameFetchOptions nullToEmpty(TaxonNameFetchOptions value) {
        return value != null ? value : TaxonNameFetchOptions.DEFAULT;
    }

    @Builder.Default
    private boolean withParentTaxonName = false;

    @Builder.Default
    private boolean withTaxonomicLevel = false;

}
