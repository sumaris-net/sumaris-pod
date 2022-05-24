package net.sumaris.extraction.core.vo;

/*-
 * #%L
 * SUMARiS:: Core Extraction
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
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

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.technical.extraction.AggregationStrataVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import org.apache.commons.collections4.ListUtils;

/**
 * @author Ludovic Pecquot <ludovic.pecquot>
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
public class AggregationProductContextVO extends AggregationContextVO {

    public AggregationProductContextVO(@NonNull ExtractionProductVO source, @NonNull String sheetName) {
        Beans.copyProperties(source, this);

        // Copy table names
        setSheetNameByTableNames(source.getTableNameBySheetNameMap());

        // Copy hidden columns
        setHiddenColumnNames(source.getHiddenColumnNames());

        // Find the strata to apply, by sheetName
        if (source.getStratum() != null) {
            AggregationStrataVO productStrata = source.getStratum().stream()
                .filter(s -> sheetName.equals(s.getSheetName()))
                .findFirst().orElse(null);
            if (productStrata != null) {
                AggregationStrataVO strata = new AggregationStrataVO(productStrata); // Copy
                this.setStrata(strata);
            }
        }
    }


}
